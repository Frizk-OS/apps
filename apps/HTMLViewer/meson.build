htmlviewer_src_dir = meson.current_source_dir()

htmlviewer_target = custom_target(
  'htmlviewer-build',
  input: [htmlviewer_src_dir / 'AndroidManifest.xml'],
  output: ['HTMLViewer.jar', 'HTMLViewer.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', htmlviewer_src_dir,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
