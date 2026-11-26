calendar_src_dir = meson.current_source_dir()

calendar_libs = [
  meson.project_source_root() / 'apps' / 'Apollo' / 'libs' / 'android-support-v4.jar',
]

calendar_aidl = [
  calendar_src_dir / 'src' / 'com' / 'android' / 'lunar' / 'ILunarService.aidl',
]

calendar_extra_srcs = [
  meson.project_source_root() / '..' / 'frameworks' / 'ex' / 'common' / 'java',
  meson.project_source_root() / '..' / 'frameworks' / 'ex' / 'chips' / 'src',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'colorpicker' / 'src',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'datetimepicker' / 'src',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'timezonepicker' / 'src',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'calendar' / 'src',
]

calendar_extra_res = [
  meson.project_source_root() / '..' / 'frameworks' / 'ex' / 'chips' / 'res',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'colorpicker' / 'res',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'datetimepicker' / 'res',
  meson.project_source_root() / '..' / 'frameworks' / 'opt' / 'timezonepicker' / 'res',
]

calendar_extra_packages = [
  'com.android.ex.chips',
  'com.android.colorpicker',
  'com.android.datetimepicker',
  'com.android.timezonepicker',
]

calendar_target = custom_target(
  'calendar-build',
  input: [calendar_src_dir / 'AndroidManifest.xml'],
  output: ['Calendar.jar', 'Calendar.apk'],
  command: [
    python, build_apk_script,
    '--sdk-dir', android_sdk,
    '--platform', android_platform,
    '--build-tools', build_tools_version,
    '--java-release', target_java_version,
    '--app-dir', calendar_src_dir,
    '--extra-src-dirs', calendar_extra_srcs,
    '--extra-res-dirs', calendar_extra_res,
    '--extra-packages', calendar_extra_packages,
    '--libs', calendar_libs,
    '--aidl-files', calendar_aidl,
    '--output-jar', '@OUTPUT0@',
    '--output-apk', '@OUTPUT1@',
    '--work-dir', '@PRIVATE_DIR@'
  ],
  build_by_default: true
)
