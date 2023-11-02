import glob
import json
import os
import shutil
import subprocess
from pathlib import Path

# if you do not use Python venv, use the second one
# vul4j_env_cmd = "source /Users/cuong/Research/Vul4J/venv/bin/activate"
vul4j_env_cmd = "cd ."

validation_results = []
validation_results_file = open("validation_results.csv", "a")


def get_first_ranked_line_folder(path_to_vul_folder):
    spectra_file = Path(path_to_vul_folder, "spectra.txt")
    with open(spectra_file, "r") as f:
        line_str = f.readline().strip()
        class_name = line_str.split("#")[0].split(".")[-1]
        line_num = line_str.split("#")[1].split(":")[1].split(",")[0]
        return Path(path_to_vul_folder, class_name + "_" + line_num)


def get_first_ranked_line_folders(path_to_vul_folder):
    spectra_file = Path(path_to_vul_folder, "spectra.txt")
    results = []
    with open(spectra_file, "r") as f:
        lines = f.read().splitlines()
        for line in lines:
            line_str = line.strip()
            class_name = line_str.split("#")[0].split(".")[-1]
            line_num = line_str.split("#")[1].split(":")[1].split(",")[0]
            results.append(Path(path_to_vul_folder, class_name + "_" + line_num))
    return results


def get_first_ranked_patch_folder(first_ranked_line_folder):
    return Path(first_ranked_line_folder, "generated_patch", "1")


def main():
    for vid in range(1, 80):
        vul_id = "VUL4J-" + str(vid)
        try:
            # checkout vul_id
            if not os.path.exists(f"/tmp/{vul_id}"):
                print("Checking out: ", vul_id)
                res = subprocess.check_output(f"{vul4j_env_cmd}; vul4j checkout --id {vul_id} -d /tmp/{vul_id}",
                                              shell=True, stdin=None, stderr=subprocess.STDOUT)
            else:
                print(f"Using the pre-checked out directory: /tmp/{vul_id}")

            vul_folder_path = Path("./vul4j/" + vul_id)
            # first_ranked_line_folder = get_first_ranked_line_folder(vul_folder_path)
            # first_ranked_patch_folder = get_first_ranked_patch_folder(first_ranked_line_folder)

            first_ranked_line_folders = get_first_ranked_line_folders(vul_folder_path)
            found = False
            for first_ranked_line_folder in first_ranked_line_folders:
                if found:
                    break

                for patch_id in range(1, 11):
                    first_ranked_patch_folder = Path(first_ranked_line_folder, "generated_patch_fixed", str(patch_id))

                    v_result = {
                        "vul_id": vul_id,
                        "patch_number": str(patch_id),
                        "patch_path": str(first_ranked_patch_folder),
                        "compilable": False,
                        "pass_povs": False,
                        "pass_all_tests": False,
                        "failed_povs": [],
                        "failed_regressions": [],
                    }

                    # inject patched file
                    print("--> Process patch:", str(first_ranked_patch_folder))
                    for patch_file in os.listdir(first_ranked_patch_folder):
                        for match_file in glob.glob(f"/tmp/{vul_id}/**/{patch_file}", recursive=True):
                            print("Injecting |==", patch_file, "==>", match_file)
                            shutil.copy2(Path(first_ranked_patch_folder, patch_file), match_file)

                    # try to compile
                    print("Compiling...", end="")
                    try:
                        res = subprocess.check_output(f"{vul4j_env_cmd}; vul4j compile -d /tmp/{vul_id}",
                                                      shell=True, stdin=None, stderr=subprocess.STDOUT)
                        v_result["compilable"] = True
                        print(u'\u2713')
                    except Exception as e:
                        print(u'\u2717' + " Compile failed!")

                    write_validation_record(v_result, validation_results_file)
                    if v_result["compilable"]:
                        found = True
                        break

                # clean the checked out folder when we are finished
                subprocess.call(f"rm -rf /tmp/{vul_id}", shell=True)
        except:
            print("Some errors encountered with ", vul_id)


def write_validation_record(v_record, file):
    vul_id = v_record["vul_id"]
    patch_number = v_record["patch_number"]
    patch_path = v_record["patch_path"]
    compilable = v_record["compilable"]
    pass_povs = v_record["pass_povs"]
    pass_all_tests = v_record["pass_all_tests"]
    failed_povs = "\"" + ",".join(v_record["failed_povs"]) + "\""
    failed_regressions = "\"" + ",".join(v_record["failed_regressions"]) + "\""

    line = f"{vul_id},{patch_number},{patch_path},{compilable},{pass_povs},{pass_all_tests},{failed_povs},{failed_regressions}\n"
    file.write(line)
    file.flush()
    print("------------------------------------------------------")


if __name__ == '__main__':
    main()
