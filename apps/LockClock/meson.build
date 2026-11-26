lockclock_src_dir = meson.current_source_dir()

lockclock_libs = [
  meson.project_source_root() / 'apps' / 'Apollo' / 'libs' / 'android-support-v4.jar',
  meson.project_source_root() / '..' / 'external' / 'google' / 'google_play_services' / 'libproject' / 'google-play-services_lib' / 'libs' / 'google-play-services.jar',
]

lockclock_target = custom_target(
  'lockclock-build',
  input: [lockclock_src_dir / 'AndroidManifest.xml'],
  output: ['LockClock.jar', 'LockClock.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', lockclock_src_dir,
    '--libs', lockclock_libs,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
