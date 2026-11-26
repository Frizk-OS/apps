calculator_src_dir = meson.current_source_dir()

calculator_libs = [
  calculator_src_dir / 'libs' / 'arity-2.1.6.jar',
  calculator_src_dir / 'libs' / 'ejml-0.21.jar',
  calculator_src_dir / 'libs' / 'gdk.jar',
  calculator_src_dir / 'libs' / 'gson-2.2.4.jar',
  calculator_src_dir / 'libs' / 'slider.jar',
  meson.project_source_root() / 'apps' / 'Apollo' / 'libs' / 'android-support-v4.jar',
  meson.project_source_root() / '../prebuilts/devtools/tools/lib/guava-13.0.1.jar',
]

calculator_target = custom_target(
  'calculator-build',
  input: [calculator_src_dir / 'AndroidManifest.xml'],
  output: ['Calculator.jar', 'Calculator.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', calculator_src_dir,
    '--libs', calculator_libs,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
