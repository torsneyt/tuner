
require 'buildr/scala'

# Version number for this release
VERSION_NUMBER = "0.9"
# Group identifier for your projects
GROUP = "tuner"
COPYRIGHT = ""

# The main class to run
MAIN_CLASS = "tuner.Tuner"

# Where are the native opengl libs
OPENGL_PATH = "lib/opengl/macosx"

# Where is R
ENV['R_HOME'] = '/Library/Frameworks/R.framework/Resources'

# Where is JRI
JRI_PATH = "/Library/Frameworks/R.framework/Versions/Current/Resources/library/rJava/jri"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://repo1.maven.org/maven2"

# All the dependencies
DATESCALA = "org.bitbucket.gabysbrain:datescala_#{Scala.version}:jar:0.9"
SCALASWING = "org.scala-lang:scala-swing:jar:#{Scala.version}"
LIFT = transitive("net.liftweb:lift-json_#{Scala.version}:jar:2.4")
TABLE_LAYOUT = "tablelayout:TableLayout:jar:20050920"
COMMONS_MATH = "org.apache.commons:commons-math:jar:2.2"
PREFUSE = "org.prefuse:prefuse:jar:beta-20060220"
JAPURA = 'org.japura:japura:jar:1.15.1'

# special artifact downloads
download artifact(JAPURA) => 'http://downloads.sourceforge.net/project/japura/Japura/Japura%20v1.15.1/japura-1.15.1.jar?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fjapura%2Ffiles%2F&ts=1343327730&use_mirror=iweb'

# all the artifacts we need
jar_deps = [
  DATESCALA, SCALASWING, LIFT, TABLE_LAYOUT, COMMONS_MATH, PREFUSE, JAPURA
]

task :macapp => :package do |t|
  @dependencies = FileList[]
  def with(*specs)
    @dependencies |= Buildr.artifacts(specs.flatten).uniq
    self
  end

  APPSHELL = "Tuner.app"
  ICON = "icon/tuner_icon.icns"

  puts "setting up mac app"
  with jar_deps, FileList.new("lib/*.jar")

  FileUtils.rm_rf "target/#{APPSHELL}"
  FileUtils.cp_r "macosx/#{APPSHELL}", "target/#{APPSHELL}"
  FileUtils.copy "target/tuner-0.9.jar", "target/#{APPSHELL}/Contents/Resources/Java/"
  FileUtils.copy ICON, "target/#{APPSHELL}/Contents/Resources/"
  # Need to copy in all the jogl stuff
  FileList.new('lib/opengl/macosx/*.jnilib').each do |f|
    FileUtils.copy f, "target/#{APPSHELL}/Contents/Resources/Java/"
  end
  # copy in the jar deps
  @dependencies.each do |dep|
    FileUtils.copy dep.to_s, "target/#{APPSHELL}/Contents/Resources/Java/"
  end
end

desc "The Tuner project"
define "tuner" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT

  # compilation info
  compile.using :deprecation => true
  compile.with jar_deps, Dir[_("lib/*.jar")]

  resources
  test.resources

  # packaging instructions
  package(:jar).with :manifest => manifest.merge('Main-Class' => MAIN_CLASS)

  # Running instructions
  run.using :main => MAIN_CLASS,
            :java_args => ["-Djava.library.path=#{JRI_PATH}:#{OPENGL_PATH}",
                           "-Xmx512M"]
end