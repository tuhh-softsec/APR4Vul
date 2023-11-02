import os
import sys
from xml.etree.ElementTree import parse


def read_test_results_maven(project_dir):
    surefire_report_files = []
    for r, dirs, files in os.walk(project_dir):
        for file in files:
            filePath = os.path.join(r, file)
            if ("target/surefire-reports" in filePath or "target/failsafe-reports" in filePath
                or "build/test-results" in filePath) and file.endswith('.xml') and file.startswith('TEST-'):
                surefire_report_files.append(filePath)

    failing_tests_count = 0
    error_tests_count = 0
    passing_tests_count = 0
    skipping_tests_count = 0

    passingTestCases = set()
    skippingTestCases = set()
    failingTestCases = set()

    failures = []

    for report_file in surefire_report_files:
        with open(report_file, 'r') as file:
            xml_tree = parse(file)
            testsuite_class_name = xml_tree.getroot().attrib['name']
            test_cases = xml_tree.findall('testcase')
            for test_case in test_cases:
                failure_list = {}
                class_name = test_case.attrib[
                    'classname'] if 'classname' in test_case.attrib else testsuite_class_name
                method_name = test_case.attrib['name'].replace("(", "").replace(")", "")
                failure_list['test_class'] = class_name
                failure_list['test_method'] = method_name

                failure = test_case.findall('failure')
                if len(failure) > 0:
                    failing_tests_count += 1
                    failure_list['failure_name'] = failure[0].attrib['type']
                    if 'message' in failure[0].attrib:
                        failure_list['detail'] = failure[0].attrib['message']
                    failure_list['is_error'] = False
                    failures.append(failure_list)
                    failingTestCases.add(class_name + "#" + method_name)
                else:
                    error = test_case.findall('error')
                    if len(error) > 0:
                        error_tests_count += 1
                        failure_list['failure_name'] = error[0].attrib['type']
                        if 'message' in error[0].attrib:
                            failure_list['detail'] = error[0].attrib['message']
                        failure_list['is_error'] = True
                        failures.append(failure_list)
                        failingTestCases.add(class_name + "#" + method_name)
                    else:
                        skipTags = test_case.findall("skipped")
                        if len(skipTags) > 0:
                            skipping_tests_count += 1
                            skippingTestCases.add(class_name + '#' + method_name)
                        else:
                            passing_tests_count += 1
                            passingTestCases.add(class_name + '#' + method_name)

    return failingTestCases


if __name__ == '__main__':
    failingTests = read_test_results_maven(sys.argv[1])
    for test in failingTests:
        print(test)
