tag_src_dir = meson.current_source_dir()

tag_libs = [
  meson.project_source_root() / '..' / 'prebuilts' / 'devtools' / 'tools' / 'lib' / 'guava-13.0.1.jar',
  meson.project_source_root() / '..' / 'prebuilts' / 'tools' / 'common' / 'm2' / 'internal' / 'com' / 'google' / 'code' / 'findbugs' / 'jsr305' / '2.0.1' / 'jsr305-2.0.1.jar',
]

tag_extra_srcs = [
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'vcard' / 'java',
]

tag_target = custom_target(
  'tag-build',
  input: [tag_src_dir / 'AndroidManifest.xml'],
  output: ['Tag.jar', 'Tag.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', tag_src_dir,
    '--extra-src-dirs', tag_extra_srcs,
    '--libs', tag_libs,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
