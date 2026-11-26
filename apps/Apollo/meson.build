apollo_src_dir = meson.current_source_dir()

apollo_libs = [
  apollo_src_dir / 'libs' / 'android-support-v4.jar'
]

apollo_aidl = [
  apollo_src_dir / 'src' / 'com' / 'andrew' / 'apollo' / 'IApolloService.aidl'
]

apollo_target = custom_target(
  'apollo-build',
  input: [apollo_src_dir / 'AndroidManifest.xml'],
  output: ['Apollo.jar', 'Apollo.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', apollo_src_dir,
    '--libs', apollo_libs,
    '--aidl-files', apollo_aidl,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
