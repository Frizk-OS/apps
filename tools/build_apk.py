#!/usr/bin/env python3
import argparse
import os
import re
import shutil
import subprocess
import sys


def parse_min_api(platform: str, explicit_min_api: str | None) -> str:
    if explicit_min_api:
        return explicit_min_api
    m = re.search(r"(\d+)$", platform)
    return m.group(1) if m else "19"


def main():
    parser = argparse.ArgumentParser(description="Build Android App with Meson and Java 21")
    parser.add_argument("--sdk-dir", required=True, help="Path to Android SDK")
    parser.add_argument("--platform", default="android-19", help="Android platform (e.g. android-19)")
    parser.add_argument("--min-api", default=None, help="Minimum Android API level for d8/apksigner")
    parser.add_argument("--build-tools", default="36.0.0", help="Build tools version")
    parser.add_argument("--java-release", default="21", help="Java release target (e.g. 21)")
    parser.add_argument("--app-dir", required=True, help="Path to app source directory")
    parser.add_argument("--extra-src-dirs", nargs="*", default=[], help="Extra Java source directories")
    parser.add_argument("--extra-res-dirs", nargs="*", default=[], help="Extra resource directories")
    parser.add_argument("--extra-packages", nargs="*", default=[], help="Extra R.java package names")
    parser.add_argument("--libs", nargs="*", default=[], help="External jar libraries")
    parser.add_argument("--aidl-files", nargs="*", default=[], help="AIDL source files")
    parser.add_argument("--cert", default=None, help="Signing certificate (.x509.pem)")
    parser.add_argument("--key", default=None, help="Signing private key (.pk8)")
    parser.add_argument("--output-jar", required=True, help="Output JAR file path")
    parser.add_argument("--output-apk", required=True, help="Output APK file path")
    parser.add_argument("--work-dir", required=True, help="Working directory for intermediates")

    args = parser.parse_args()

    min_api = parse_min_api(args.platform, args.min_api)
    sdk_dir = os.path.abspath(args.sdk_dir)
    build_tools_dir = os.path.join(sdk_dir, "build-tools", args.build_tools)
    platform_dir = os.path.join(sdk_dir, "platforms", args.platform)
    android_jar = os.path.join(platform_dir, "android.jar")
    framework_aidl = os.path.join(platform_dir, "framework.aidl")

    aapt = os.path.join(build_tools_dir, "aapt")
    aidl = os.path.join(build_tools_dir, "aidl")
    d8 = os.path.join(build_tools_dir, "d8")
    zipalign = os.path.join(build_tools_dir, "zipalign")
    apksigner = os.path.join(build_tools_dir, "apksigner")

    app_dir = os.path.abspath(args.app_dir)
    manifest = os.path.join(app_dir, "AndroidManifest.xml")
    res_dirs = [os.path.join(app_dir, "res")] + [os.path.abspath(d) for d in args.extra_res_dirs]
    res_dirs = [d for d in res_dirs if os.path.isdir(d)]
    assets_dir = os.path.join(app_dir, "assets")

    work_dir = os.path.abspath(args.work_dir)
    gen_dir = os.path.join(work_dir, "gen")
    classes_dir = os.path.join(work_dir, "classes")
    dex_dir = os.path.join(work_dir, "dex")

    shutil.rmtree(work_dir, ignore_errors=True)
    os.makedirs(gen_dir, exist_ok=True)
    os.makedirs(classes_dir, exist_ok=True)
    os.makedirs(dex_dir, exist_ok=True)
    os.makedirs(os.path.dirname(os.path.abspath(args.output_jar)), exist_ok=True)
    os.makedirs(os.path.dirname(os.path.abspath(args.output_apk)), exist_ok=True)

    print(f"=== Compiling AIDL files for {os.path.basename(app_dir)} ===")
    for aidl_file in args.aidl_files:
        aidl_path = os.path.abspath(aidl_file)
        cmd = [
            aidl,
            f"-p{framework_aidl}",
            f"-I{os.path.join(app_dir, 'src')}",
            aidl_path,
            os.path.join(gen_dir, os.path.splitext(os.path.basename(aidl_path))[0] + ".java"),
        ]
        subprocess.check_call(cmd)

    print("=== Generating R.java via AAPT ===")
    cmd = [
        aapt, "package", "-f", "-m", "--auto-add-overlay",
        "-J", gen_dir,
        "-M", manifest,
        "-I", android_jar,
    ]
    for res_dir in res_dirs:
        cmd.extend(["-S", res_dir])
    if args.extra_packages:
        cmd.extend(["--extra-packages", ":".join(args.extra_packages)])
    subprocess.check_call(cmd)

    print("=== Gathering Java sources ===")
    src_roots = [os.path.join(app_dir, "src")] + [os.path.abspath(d) for d in args.extra_src_dirs] + [gen_dir]
    java_files = []
    for src_root in src_roots:
        if not os.path.isdir(src_root):
            continue
        for root, _, files in os.walk(src_root):
            for f in files:
                if f.endswith(".java"):
                    java_files.append(os.path.join(root, f))

    sources_file = os.path.join(work_dir, "sources.txt")
    with open(sources_file, "w") as f:
        f.write("\n".join(java_files) + "\n")

    classpath = [android_jar] + [os.path.abspath(lib) for lib in args.libs]
    cp_str = os.pathsep.join(classpath)

    print(f"=== Compiling with javac (target Java {args.java_release}) ===")
    javac_cmd = [
        "javac",
        "--release", args.java_release,
        "-cp", cp_str,
        "-d", classes_dir,
        f"@{sources_file}",
    ]
    subprocess.check_call(javac_cmd)

    print(f"=== Creating JAR: {args.output_jar} ===")
    jar_cmd = [
        "jar", "cf", os.path.abspath(args.output_jar),
        "-C", classes_dir, ".",
    ]
    subprocess.check_call(jar_cmd)

    print(f"=== Compiling DEX with d8 (min-api {min_api}) ===")
    d8_cmd = [
        d8,
        "--min-api", min_api,
        "--lib", android_jar,
        "--output", dex_dir,
    ]
    for root, _, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                d8_cmd.append(os.path.join(root, f))
    for lib in args.libs:
        d8_cmd.append(os.path.abspath(lib))
    subprocess.check_call(d8_cmd)

    print("=== Packaging APK with AAPT ===")
    unaligned_apk = os.path.join(work_dir, "unaligned.apk")
    aapt_cmd = [
        aapt, "package", "-f", "--auto-add-overlay",
        "-M", manifest,
        "-I", android_jar,
        "-F", unaligned_apk,
    ]
    for res_dir in res_dirs:
        aapt_cmd.extend(["-S", res_dir])
    if os.path.isdir(assets_dir):
        aapt_cmd.extend(["-A", assets_dir])
    subprocess.check_call(aapt_cmd)

    print("=== Adding DEX to APK ===")
    subprocess.check_call([aapt, "add", unaligned_apk, "classes.dex"], cwd=dex_dir)

    print("=== Aligning APK with zipalign ===")
    subprocess.check_call([zipalign, "-f", "-p", "4", unaligned_apk, os.path.abspath(args.output_apk)])

    default_cert = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "build", "target", "product", "security", "testkey.x509.pem"))
    default_key = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "build", "target", "product", "security", "testkey.pk8"))
    cert_path = os.path.abspath(args.cert) if args.cert else (default_cert if os.path.exists(default_cert) else None)
    key_path = os.path.abspath(args.key) if args.key else (default_key if os.path.exists(default_key) else None)

    print("=== Signing APK with apksigner ===")
    if cert_path and key_path:
        sign_cmd = [
            apksigner, "sign",
            "--min-sdk-version", min_api,
            "--cert", cert_path,
            "--key", key_path,
            os.path.abspath(args.output_apk),
        ]
    else:
        keystore = os.path.join(os.path.dirname(work_dir), "debug.keystore")
        if not os.path.exists(keystore):
            print("=== Generating debug keystore ===")
            keytool_cmd = [
                "keytool", "-genkeypair", "-v",
                "-keystore", keystore,
                "-storepass", "android",
                "-alias", "androiddebugkey",
                "-keypass", "android",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Android Debug,O=Android,C=US",
            ]
            subprocess.check_call(keytool_cmd)
        sign_cmd = [
            apksigner, "sign",
            "--min-sdk-version", min_api,
            "--ks", keystore,
            "--ks-pass", "pass:android",
            "--key-pass", "pass:android",
            "--ks-key-alias", "androiddebugkey",
            os.path.abspath(args.output_apk),
        ]
    subprocess.check_call(sign_cmd)

    print("=== SUCCESS: Build complete! ===")
    print(f"JAR: {args.output_jar} ({os.path.getsize(args.output_jar)} bytes)")
    print(f"APK: {args.output_apk} ({os.path.getsize(args.output_apk)} bytes)")


if __name__ == "__main__":
    main()
