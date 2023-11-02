import csv
import datetime
import json
import os
import re
import shutil
import subprocess

from config import REPAIR_ROOT, DATA_PATH, JAVA8_HOME, MAVEN_BIN, WORKING_DIRECTORY, JAVA7_HOME, MAVEN_OPTS, OUTPUT_PATH
from core.Benchmark import Benchmark
from core.Bug import Bug
from core.utils import add_benchmark

FNULL = open(os.devnull, 'w')


def abs_to_rel(root, folders):
    if root[-1] != '/':
        root += "/"
    output = []
    for folder in folders:
        output.append(folder.replace(root, ""))
    return output


class VUL4J(Benchmark):
    def __init__(self):
        super(VUL4J, self).__init__("vul4j")
        self.path = os.path.join(REPAIR_ROOT, "benchmarks", "vul4j")
        self.bugs = None
        self.get_bugs()

    def get_data_path(self):
        return os.path.join(DATA_PATH, "benchmarks", "vul4j")

    def get_bugs(self):
        if self.bugs is not None:
            return self.bugs
        self.bugs = []

        # get all the bugs of the benchmark
        with open(os.path.join(self.get_data_path(), "vul4j.csv")) as f:
            reader = csv.DictReader(f, delimiter=',', )
            for row in reader:
                bug_id = row['vul_id'].strip()
                repo_slug = row['repo_slug'].strip().replace("/", "_")

                self.bugs += [Bug(self, repo_slug, bug_id)]
        return self.bugs

    def get_bug_metadata(self, bug, key):
        with open(os.path.join(self.get_data_path(), "vul4j.csv")) as f:
            reader = csv.DictReader(f, delimiter=',', )
            for row in reader:
                bug_id = row['vul_id'].strip()
                if bug_id == bug.bug_id:
                    return row[key].strip()
        return None

    def get_bug(self, bug_id):
        # get a bug based on its id
        for bug in self.get_bugs():
            if bug_id.lower() == bug.bug_id.lower():
                return bug
        return None

    def checkout(self, bug, working_directory, buggy_version=True):
        # checkout a buggy version
        branch_id = bug.bug_id
        cmd = "cd " + self.path + "; git reset .; git checkout -- .; git clean -x -d --force; git checkout -f " + branch_id
        subprocess.call(cmd, shell=True, stdout=FNULL, stderr=subprocess.STDOUT)

        # copy code from buggy version into the working directory
        cmd = """cd %s; cp -r . %s""" % (
            self.path,
            working_directory,
        )
        subprocess.call(cmd, shell=True, stdout=FNULL, stderr=subprocess.STDOUT)

        cmd = """cp %s %s""" % (os.path.join(self.get_data_path(), "read_test_results.py"),  working_directory)
        subprocess.call(cmd, shell=True, stdout=FNULL, stderr=subprocess.STDOUT)

        pass

    def compile(self, bug, working_directory):
        # compile a bug
        java_version = os.path.join(JAVA8_HOME, '..')
        if self.get_bug_metadata(bug, 'compliance_level') == '7':
            java_version = os.path.join(JAVA7_HOME, '..')

        cmd = """cd %s;
export JAVA_HOME="%s";
export PATH="%s:$PATH";
export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
export MAVEN_OPTS="%s";
%s;""" % (working_directory, java_version, MAVEN_BIN, MAVEN_OPTS,
               self.get_bug_metadata(bug, 'compile_cmd'))

        cmd_options = self.get_bug_metadata(bug, 'cmd_options')
        if cmd_options:
            cmd = cmd[:-1]  # remove comma
            cmd += " " + cmd_options + ';'

        log_path = os.path.join(OUTPUT_PATH, self.name, bug.project, str(bug.bug_id), "compile.log")
        if not os.path.exists(os.path.dirname(log_path)):
            os.makedirs(os.path.dirname(log_path))
        with open(log_path, "w") as log_file:
            log_file.write(cmd)
            subprocess.call(cmd, shell=True, stdout=log_file, stderr=subprocess.STDOUT)
        pass

    def run_test(self, bug, working_directory, test=None):
        # run the test of a bug
        java_version = os.path.join(JAVA8_HOME, '..')
        if self.get_bug_metadata(bug, 'compliance_level') == '7':
            java_version = os.path.join(JAVA7_HOME, '..')

        cmd = """cd %s;
export JAVA_HOME="%s";
export PATH="%s:$PATH";
export _JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true;
export MAVEN_OPTS="%s";
%s;""" % (working_directory, java_version, MAVEN_BIN, MAVEN_OPTS,
                  self.get_bug_metadata(bug, 'test_all_cmd'))

        cmd_options = self.get_bug_metadata(bug, 'cmd_options')
        if cmd_options:
            cmd = cmd[:-1]  # remove comma
            cmd += " " + cmd_options + ';'

        log_path = os.path.join(OUTPUT_PATH, self.name, bug.project, str(bug.bug_id), "testing.log")
        if not os.path.exists(os.path.dirname(log_path)):
            os.makedirs(os.path.dirname(log_path))
        with open(log_path, "w") as log_file:
            log_file.write(cmd)
            subprocess.call(cmd, shell=True, stdout=log_file, stderr=subprocess.STDOUT)
        pass

    def test_failing_cmd(self, bug):
        cmd = self.get_bug_metadata(bug, 'test_cmd') + ";"
        cmd_options = self.get_bug_metadata(bug, 'cmd_options')
        if cmd_options:
            cmd = cmd[:-1]  # remove comma
            cmd += " " + cmd_options + ';'
        return cmd

    def test_all_cmd(self, bug):
        cmd = self.get_bug_metadata(bug, 'test_all_cmd') + ";"
        cmd_options = self.get_bug_metadata(bug, 'cmd_options')
        if cmd_options:
            cmd = cmd[:-1]  # remove comma
            cmd += " " + cmd_options + ';'
        return cmd

    def failing_tests(self, bug):
        tests = ([f.strip() for f in self.get_bug_metadata(bug, 'failing_tests').split(',')])
        return tests

    def ignored_tests(self, bug):
        tests = ([f.strip() for f in self.get_bug_metadata(bug, 'ignored_tests').split(',')])
        return tests

    def failing_module(self, bug):
        module = self.get_bug_metadata(bug, 'failing_module')
        if module:
            return module
        return 'root'

    def source_folders(self, bug):
        folders = ([f.strip() for f in self.get_bug_metadata(bug, 'src').split(',')])
        failing_module = self.failing_module(bug)

        if failing_module == 'root':
            return folders

        folders = map(lambda folder: os.path.join(failing_module, folder), folders)
        return folders

    def test_folders(self, bug):
        folders = ([f.strip() for f in self.get_bug_metadata(bug, 'test').split(',')])
        failing_module = self.failing_module(bug)

        if failing_module == 'root':
            return folders

        folders = map(lambda folder: os.path.join(failing_module, folder), folders)
        return folders

    def bin_folders(self, bug):
        folders = ([f.strip() for f in self.get_bug_metadata(bug, 'src_classes').split(',')])
        failing_module = self.failing_module(bug)

        if failing_module == 'root':
            return folders

        folders = map(lambda folder: os.path.join(failing_module, folder), folders)
        return folders

    def test_bin_folders(self, bug):
        folders = ([f.strip() for f in self.get_bug_metadata(bug, 'test_classes').split(',')])
        failing_module = self.failing_module(bug)

        if failing_module == 'root':
            return folders

        folders = map(lambda folder: os.path.join(failing_module, folder), folders)
        return folders

    def classpath(self, bug):
        if self.get_bug_metadata(bug, 'build_system') == "Gradle":
            test_all_cmd = self.get_bug_metadata(bug, 'test_all_cmd')
            gradle_classpath_cmd = './gradlew copyClasspath'
            classpath_info_file = 'classpath.info'

            matched = re.search("(./gradlew :.*:)test$", test_all_cmd)
            if matched is None:
                if test_all_cmd.strip() != "./gradlew test":
                    print("The test all command should follow the regex \"(./gradlew :.*:)test$\"!"
                          " It is now %s." % test_all_cmd)
                    exit(1)
            else:
                gradle_classpath_cmd = matched.group(1) + "copyClasspath"
                classpath_info_file = os.path.join(self.get_bug_metadata(bug, 'failing_module'), 'classpath.info')

            cat_classpath_info_cmd = "cat " + classpath_info_file
            cp_cmd = "%s;%s" % (gradle_classpath_cmd, cat_classpath_info_cmd)

            java_home = os.path.join(JAVA8_HOME, '..')
            if self.get_bug_metadata(bug, 'compliance_level') == '7':
                java_home = os.path.join(JAVA7_HOME, '..')

            cmd = """cd %s;
                    export JAVA_HOME="%s";
                    %s;""" % (bug.working_directory, java_home, cp_cmd.split(';')[0])

            subprocess.call(cmd, shell=True, stdout=FNULL, stderr=subprocess.STDOUT)

            classpath = subprocess.check_output(
                "cd %s;%s;" % (bug.working_directory, cp_cmd.split(';')[1]), shell=True)
            return classpath
        else:
            info = self._get_project_info(bug)  # automatically extract classpath for Maven projects
            failing_module = self.failing_module(bug)

            deps = []

            for module in info['modules']:
                module_name = os.path.basename(module['baseDir'])
                if failing_module != module_name and failing_module != module['name']:
                    deps += module['binSources']
            deps += info['classpath']

            return ":".join(deps)

    def compliance_level(self, bug):
        return int(self.get_bug_metadata(bug, 'compliance_level'))


add_benchmark("vul4j", VUL4J)
