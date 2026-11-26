basicsms_src_dir = meson.current_source_dir()

basicsms_target = custom_target(
  'basicsms-build',
  input: [basicsms_src_dir / 'AndroidManifest.xml'],
  output: ['BasicSmsReceiver.jar', 'BasicSmsReceiver.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', basicsms_src_dir,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
