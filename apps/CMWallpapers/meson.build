cmwallpapers_src_dir = meson.current_source_dir()

cmwallpapers_target = custom_target(
  'cmwallpapers-build',
  input: [cmwallpapers_src_dir / 'AndroidManifest.xml'],
  output: ['CMWallpapers.jar', 'CMWallpapers.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', cmwallpapers_src_dir,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
