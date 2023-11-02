import argparse
import json
import os
import subprocess
from xml.etree.ElementTree import parse

from config import WORKING_DIRECTORY, OUTPUT_PATH
from core.utils import get_benchmark

parser = argparse.ArgumentParser(prog="info_json_file", description='Get info json file')
parser.add_argument("--benchmark", "-b", required=True, default="Defects4J",
                    help="The benchmark to repair")
parser.add_argument("--id", "-i", nargs='+', help="The bug id")

if __name__ == "__main__":
    args = parser.parse_args()
    if not os.path.exists(os.path.join(OUTPUT_PATH, 'testResults')):
        os.makedirs(os.path.join(OUTPUT_PATH, 'testResults'))

    args.benchmark = get_benchmark(args.benchmark)

    bugs = args.benchmark.get_bugs()
    if args.id is not None:
        bugs = []
        for bug_id in args.id:
            bugs.append(args.benchmark.get_bug(bug_id))

    count = 0
    for bug in bugs:
        count += 1
        print "==[%s/%s]=====> Running test validation for %s..." % (count, len(bugs), bug.bug_id)
        bug_path = os.path.join(WORKING_DIRECTORY, "%s_%s_%s" % (bug.benchmark.name, bug.project, bug.bug_id))
        bug.checkout(bug_path)
        bug.compile()
        bug.run_test()

        reportFiles = []
        for root, dirs, files in os.walk(bug_path):
            for file in files:
                filePath = os.path.join(root, file)
                if "target/surefire-reports" in filePath and file.endswith('.xml') and file.startswith('TEST-'):
                    reportFiles.append(filePath)

        passingTests = 0
        skippingTests = 0
        failingTests = 0
        erroringTests = 0
        failureDetails = []
        passingTestCases = set()
        skippingTestCases = set()
        for xmlFile in reportFiles:
            with open(xmlFile, 'r') as file:
                xmlTree = parse(file)
                testSuiteClassName = xmlTree.getroot().attrib['name']
                testCases = xmlTree.findall('testcase')
                for testCase in testCases:
                    failureDetail = {}
                    className = testCase.attrib['classname'] if 'classname' in testCase.attrib else testSuiteClassName
                    methodName = testCase.attrib['name']
                    failureDetail['testClass'] = className
                    failureDetail['testMethod'] = methodName

                    failure = testCase.findall('failure')
                    if len(failure) > 0:
                        failingTests += 1
                        failureDetail['failureName'] = failure[0].attrib['type']
                        if 'message' in failure[0].attrib:
                            failureDetail['detail'] = failure[0].attrib['message']
                        failureDetail['isError'] = False
                        failureDetails.append(failureDetail)
                    else:
                        error = testCase.findall('error')
                        if len(error) > 0:
                            erroringTests += 1
                            failureDetail['failureName'] = error[0].attrib['type']
                            if 'message' in error[0].attrib:
                                failureDetail['detail'] = error[0].attrib['message']
                            failureDetail['isError'] = True
                            failureDetails.append(failureDetail)
                        else:
                            skipTags = testCase.findall("skipped")
                            if len(skipTags) > 0:
                                skippingTests += 1
                                if '[' in methodName:
                                    methodName = methodName[0:methodName.find('[')]
                                skippingTestCases.add(className + '#' + methodName)
                            else:
                                passingTests += 1
                                methodName = testCase.attrib['name']
                                if '[' in methodName:
                                    methodName = methodName[0:methodName.find('[')]
                                passingTestCases.add(className + '#' + methodName)

        jsonFile = {}
        jsonFile['benchmark'] = bug.benchmark.name
        jsonFile['bugId'] = bug.bug_id

        repository = {}
        repository['name'] = bug.project
        jsonFile['repository'] = repository

        if bug.benchmark.name == 'Defects4J' or bug.benchmark.name == 'Bugs.jar' or bug.benchmark.name == 'Bears':
            projectInfo = bug.benchmark._get_project_info(bug)
            projectMetrics = {}
            if projectInfo is not None:
                projectMetrics['numberModules'] = len(projectInfo['modules'])
            jsonFile['projectMetrics'] = projectMetrics

        tests = {}
        overallMetrics = {}
        overallMetrics['numberPassing'] = passingTests
        overallMetrics['numberSkipping'] = skippingTests
        overallMetrics['numberRunning'] = passingTests + failingTests + erroringTests
        overallMetrics['numberFailing'] = failingTests
        overallMetrics['numberErroring'] = erroringTests
        tests['overallMetrics'] = overallMetrics
        tests['failureDetails'] = failureDetails
        tests['passingTests'] = list(passingTestCases)
        tests['skippingTests'] = list(skippingTestCases)
        jsonFile['tests'] = tests

        with open(os.path.join(OUTPUT_PATH, 'testResults',
                               "%s_%s_%s" % (bug.benchmark.name, bug.project, bug.bug_id) + '.json'), 'w') as f:
            f.write(json.dumps(jsonFile, indent=2, sort_keys=True))

        # print(json.dumps(jsonFile, indent=1, sort_keys=True))

        cmd = "rm -rf %s;" % bug_path
        subprocess.call(cmd, shell=True)
