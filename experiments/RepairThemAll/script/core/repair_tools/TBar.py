import json
import os
import datetime
import subprocess

from config import WORKING_DIRECTORY, REPAIR_ROOT, JAVA7_HOME, JAVA8_HOME, JAVA_ARGS, TOOL_TIMEOUT, FL_MODE
from core.RepairTool import RepairTool
from core.utils import add_repair_tool
from core.runner.RepairTask import RepairTask


def to_absolute(root, folders):
    absolute_folders = []
    for folder in folders:
        if os.path.exists(os.path.join(root, folder)):
            absolute_folders.append(os.path.join(root, folder))
    return absolute_folders


class TBar(RepairTool):
    """TBar"""

    def __init__(self, name="TBar"):
        super(TBar, self).__init__(name, "tbar")

    def repair(self, repair_task):
        """"
        :type repair_task: RepairTask
        """
        bug = repair_task.bug
        bug_path = os.path.join(WORKING_DIRECTORY,
                                "%s_%s_%s_%s" % (self.name, bug.benchmark.name, bug.project, bug.bug_id))
        repair_task.working_directory = bug_path
        self.init_bug(bug, bug_path)

        try:
            log_path = os.path.join(repair_task.log_dir(), "repair.log")
            if not os.path.exists(os.path.dirname(log_path)):
                os.makedirs(os.path.dirname(log_path))

            classpath = bug.classpath()
            if classpath == "":
                classpath = '""'
            bin_folders = self.refine_path(bug.bin_folders()[0])
            test_bin_folders = self.refine_path(bug.test_bin_folders()[0])
            sources = self.refine_path(bug.source_folders()[0])
            tests = self.refine_path(bug.test_folders()[0])
            failing_tests = bug.failing_tests()

            fl_file = ""
            if FL_MODE != "":
                fl_file = bug.benchmark.get_data_path() + "/" + FL_MODE + "/%s/spectra.txt" % bug.bug_id

            cmd_test_failing = bug.benchmark.test_failing_cmd(bug).replace(";", "")
            cmd_test_all = bug.benchmark.test_all_cmd(bug).replace(";", "")

            java_version = JAVA7_HOME
            if bug.compliance_level() > 7:
                java_version = JAVA8_HOME
            cmd = """cd %s;
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8 -Duser.language=en-US -Duser.country=US -Duser.language=en";
TZ="America/New_York"; export TZ;
export PATH="%s:$PATH";
export JAVA_HOME="%s";
timeout %sm java %s \\
    -cp %s %s \\
    "%s" "%s" "%s" "%s" "%s" "%s" "%s" "%s" "%s" "%s";
echo "\\n\\nNode: `hostname`\\n";
echo "\\n\\nDate: `date`\\n";
""" % (bug_path,
        java_version,
        java_version.replace("/bin", ""),
        TOOL_TIMEOUT,
        JAVA_ARGS,
        os.path.join(REPAIR_ROOT, "libs", "tbar_external", "com.gzoltar-0.1.1-jdk7.jar") + ":" + self.jar,
        self.main,
        bug_path, # project path
        ','.join(failing_tests), # failing tests
        sources, tests, bin_folders, test_bin_folders,
        fl_file,
        cmd_test_failing,
        cmd_test_all,
        classpath)
            log = file(log_path, 'w')
            log.write(cmd)
            log.flush()

            subprocess.call(cmd, shell=True, stdout=log, stderr=subprocess.STDOUT)
            with open(log_path) as data_file:
                return data_file.read()
        finally:
            result = {
                "repair_begin": self.repair_begin,
                "repair_end": datetime.datetime.now().__str__(),
                "patches": []
            }
            repair_task.status = "FINISHED"

            output_folder = os.path.join(bug_path, "OUTPUT")
            if os.path.exists(output_folder):
                for root, subdirs, files in os.walk(output_folder):
                    for f in files:
                        path_f = os.path.join(root, f)
                        if ".txt" not in f:
                            continue
                        patch = {
                            "edits": []
                        }
                        with open(path_f) as fd:
                            patch["patch"] = fd.read()
                        result["patches"].append(patch)

            with open(os.path.join(repair_task.log_dir(), "result.json"), "w+") as fd2:
                json.dump(result, fd2, indent=2)
            repair_task.results = result
            if len(result['patches']) > 0:
                repair_task.status = "PATCHED"
            cmd = "rm -rf %s;" % bug_path
            subprocess.call(cmd, shell=True)

    pass

    def refine_path(self, path):
        if not path.startswith('/'):
            path = '/' + path

        if not path.endswith('/'):
            path = path + '/'
        return path

def init(args):
    return TBar()


def tbar_args(parser):
    pass


parser = add_repair_tool('TBar', init, 'Repair the bug with TBar')
tbar_args(parser)
