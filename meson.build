project('cm11-packages',
  version: '11.0.0',
  meson_version: '>= 0.60.0'
)

python = import('python').find_installation('python3')

android_sdk = get_option('android_sdk')
android_platform = get_option('android_platform')
build_tools_version = get_option('build_tools_version')
target_java_version = get_option('target_java_version')

build_apk_script = meson.project_source_root() / 'tools' / 'build_apk.py'

subdir('apps/Apollo')
subdir('apps/BasicSmsReceiver')
subdir('apps/Calculator')
subdir('apps/Calendar')
subdir('apps/CMWallpapers')
subdir('apps/HTMLViewer')
subdir('apps/LockClock')
subdir('apps/OneTimeInitializer')
subdir('apps/Tag')
