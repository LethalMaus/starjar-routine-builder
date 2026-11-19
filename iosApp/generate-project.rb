# Run with Ruby and the xcodeproj gem. The Gradle framework is built separately.
gem 'rexml', '3.4.1'
require 'xcodeproj'
Dir.chdir(__dir__)
project = Xcodeproj::Project.new('RoutineBuilder.xcodeproj')
project.add_build_configuration('Evaluation', :release)
target = project.new_target(:application, 'RoutineBuilder', :ios, '16.0')
%w[App.swift NativeBridge.swift].each { |path| target.source_build_phase.add_file_reference(project.main_group.new_file(path)) }
project.main_group.new_file('Info.plist')
target.resources_build_phase.add_file_reference(project.main_group.new_file('Assets.xcassets'))
target.resources_build_phase.add_file_reference(project.main_group.new_file('PrivacyInfo.xcprivacy'))
framework = project.main_group.new_file('../shared/build/bin/$(KOTLIN_TARGET)/$(KOTLIN_BUILD)Framework/RoutineShared.framework')
target.frameworks_build_phase.add_file_reference(framework)
package = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
package.repositoryURL = 'https://github.com/firebase/firebase-ios-sdk.git'
package.requirement = { 'kind' => 'exactVersion', 'version' => '12.19.3' }
project.root_object.package_references << package
%w[FirebaseAI FirebaseAppCheck].each do |name|
  product = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
  product.package = package
  product.product_name = name
  target.package_product_dependencies << product
  file = project.new(Xcodeproj::Project::Object::PBXBuildFile)
  file.product_ref = product
  target.frameworks_build_phase.files << file
end
recaptcha = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
recaptcha.repositoryURL = 'https://github.com/GoogleCloudPlatform/recaptcha-enterprise-mobile-sdk.git'
recaptcha.requirement = { 'kind' => 'exactVersion', 'version' => '18.9.0' }
project.root_object.package_references << recaptcha
product = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
product.package = recaptcha
product.product_name = 'RecaptchaEnterprise'
target.package_product_dependencies << product
file = project.new(Xcodeproj::Project::Object::PBXBuildFile)
file.product_ref = product
target.frameworks_build_phase.files << file
signing_reference = project.main_group.new_file('Signing.xcconfig')
target.build_configurations.each do |config|
  config.base_configuration_reference = signing_reference
  s = config.build_settings
  s['PRODUCT_BUNDLE_IDENTIFIER'] = 'dev.jamescullimore.starjar.routinebuilder'
  s['DEVELOPMENT_TEAM'] = 'LFWZYL42HG'
  s['SWIFT_VERSION'] = '5.0'
  s['ASSETCATALOG_COMPILER_APPICON_NAME'] = 'AppIcon'
  s['ROUTINE_JUDGING_ACCESS'] = config.name == 'Evaluation' ? 'YES' : 'NO'
  s['CODE_SIGN_ENTITLEMENTS'] = 'RoutineBuilder.entitlements' unless config.name == 'Debug'
  s['ARCHS[sdk=iphonesimulator*]'] = 'arm64'
  s['INFOPLIST_FILE'] = 'Info.plist'
  s['TARGETED_DEVICE_FAMILY'] = '1,2'
  s['SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD'] = 'NO'
  s['SUPPORTS_MACCATALYST'] = 'NO'
  s['SUPPORTED_PLATFORMS'] = 'iphoneos iphonesimulator'
  s['ENABLE_USER_SCRIPT_SANDBOXING'] = 'YES'
  s['FRAMEWORK_SEARCH_PATHS'] = '$(inherited) "$(SRCROOT)/../shared/build/bin/$(KOTLIN_TARGET)/$(KOTLIN_BUILD)Framework"'
  s['KOTLIN_TARGET[sdk=iphonesimulator*]'] = 'iosSimulatorArm64'
  s['KOTLIN_TARGET[sdk=iphoneos*]'] = 'iosArm64'
  s['KOTLIN_BUILD'] = config.name == 'Debug' ? 'debug' : 'release'
  s['OTHER_LDFLAGS'] = '$(inherited) -framework RoutineShared -lsqlite3 -ObjC'
  s['SWIFT_ACTIVE_COMPILATION_CONDITIONS'] = config.name == 'Debug' ? 'DEBUG' : (config.name == 'Evaluation' ? 'ATTESTATION_TEST' : '')
end
# Private Firebase configuration is optional so public source builds without credentials.
if File.exist?('AppCheck.local.plist')
 target.resources_build_phase.add_file_reference(project.main_group.new_file('AppCheck.local.plist'))
end
if File.exist?('GoogleService-Info.plist')
 target.resources_build_phase.add_file_reference(project.main_group.new_file('GoogleService-Info.plist'))
end
resources = project.main_group.new_file('../shared/build/ios/compose-resources')
resources.last_known_file_type = 'folder'
target.resources_build_phase.add_file_reference(resources)
tests = project.new_target(:ui_test_bundle, 'RoutineBuilderUITests', :ios, '16.0')
tests.add_dependency(target)
tests.source_build_phase.add_file_reference(project.main_group.new_file('SmokeTests.swift'))
tests.build_configurations.each do |config|
 config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'dev.jamescullimore.starjar.routinebuilder.uitests'
 config.build_settings['TEST_TARGET_NAME'] = 'RoutineBuilder'
 config.build_settings['SWIFT_VERSION'] = '5.0'
 config.build_settings['ARCHS[sdk=iphonesimulator*]'] = 'arm64'
 config.build_settings['GENERATE_INFOPLIST_FILE'] = 'YES'
 config.build_settings['DEVELOPMENT_TEAM'] = 'LFWZYL42HG'
end
live = project.new_target(:unit_test_bundle, 'RoutineBuilderLiveAITests', :ios, '16.0')
live.add_dependency(target)
live.source_build_phase.add_file_reference(project.main_group.new_file('LiveAITests.swift'))
live.source_build_phase.add_file_reference(project.main_group.new_file('AccessPolicyTests.swift'))
live.build_configurations.each do |config|
 config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] = 'dev.jamescullimore.starjar.routinebuilder.liveaitests'
 config.build_settings['SWIFT_VERSION'] = '5.0'
 config.build_settings['ARCHS[sdk=iphonesimulator*]'] = 'arm64'
 config.build_settings['GENERATE_INFOPLIST_FILE'] = 'YES'
 config.build_settings['TEST_HOST'] = '$(BUILT_PRODUCTS_DIR)/RoutineBuilder.app/RoutineBuilder'
 config.build_settings['BUNDLE_LOADER'] = '$(TEST_HOST)'
 config.build_settings['DEVELOPMENT_TEAM'] = 'LFWZYL42HG'
 config.build_settings['FRAMEWORK_SEARCH_PATHS'] = '$(inherited) "$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework"'
end
Xcodeproj::XCScheme.new.tap { |scheme| scheme.add_build_target(target); scheme.set_launch_target(target); scheme.add_test_target(live); scheme.save_as(project.path, 'RoutineBuilderLiveAI', true) }
Xcodeproj::XCScheme.new.tap { |scheme| scheme.add_build_target(target); scheme.set_launch_target(target); scheme.add_test_target(tests); scheme.save_as(project.path, 'RoutineBuilder', true) }
project.save
