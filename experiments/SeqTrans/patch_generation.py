import subprocess
import sys
import os
import traceback
from pathlib import Path


def main(argv):
    buggy_file_lines = open(argv[0], "r").readlines()
    buggy_line_number = int(argv[1])
    buggy_line = buggy_file_lines[buggy_line_number-1]
    predictions = open(argv[2], "r").readlines()
    reco_lines = open(argv[3], "r").readlines()
    starting_pred_pos = int(argv[5])
    if len(reco_lines) > 0:
        recover_line = reco_lines[0]
        recovers = recover_line.split(";")
    else:
        recovers = []

    recover_dic = {}
    str_list = []
    if len(recovers) != 0:
        for recover in recovers:
            tokens = recover.split("->")
            if len(tokens) == 2:
                if tokens[1] != "str":
                    recover_dic[tokens[1]] = tokens[0]
                else:
                    str_list.append(tokens[0])
    predictions_recover = []
    for predict in predictions:
        tokens = predict.split(" ")
        recover_line = ""
        str_num = 1
        count = 0
        for token in tokens:
            if token in recover_dic.keys():
                token = recover_dic[token]
            elif token == "str":
                if str_num <= len(str_list):
                    token = str_list[str_num-1]
                    str_num += 1
                else:
                    str_num += 1
            if count + 1 < len(tokens) and tokens[count + 1] == "=" and count - 1 >= 0 and tokens[count - 1] != ".":
                recover_line += " " + token
            elif token == "private" or token == "static" or token == "final":
                recover_line += token + " "
            else:
                recover_line += token
            count += 1
        predictions_recover.append(recover_line)
    white_space_before_buggy_line = buggy_line[0:buggy_line.find(buggy_line.lstrip())]
    for i in range(starting_pred_pos, starting_pred_pos + 10):
        output_file = os.path.join(argv[4], str(i+1), os.path.basename(argv[0]))
        os.makedirs(os.path.dirname(output_file))
        output_file = open(output_file, "w")
        for j in range(len(buggy_file_lines)):
            if(j+1 == buggy_line_number):
                last_char = buggy_file_lines[j].rstrip()[-1]
                if predictions_recover[i][-1] != last_char:
                    output_file.write(white_space_before_buggy_line + predictions_recover[i].rstrip()+last_char+"\n")
                else:
                    output_file.write(white_space_before_buggy_line + predictions_recover[i])
            else:
                output_file.write(buggy_file_lines[j])
        output_file.close()


if __name__ == "__main__":
    start_id = "VUL4J-1"
    cont = True
    for i in range(1, 80):
        vul_id = "VUL4J-" + str(i)
        if vul_id != start_id:
            if cont:
                continue
        else:
            cont = False

        root_path = "./vul4j/" + vul_id + "/"

        for file in os.listdir(root_path):
            vul_case_path = root_path + "/" + file + "/"

            if os.path.isdir(vul_case_path)\
                    and file != "patch" and file != "generated_patch" \
                    and "_" in file:

                generated_patch_path = vul_case_path + "/generated_patch"
                Path(generated_patch_path).mkdir(parents=False, exist_ok=True)

                count1 = 0
                with open(vul_case_path + "src-num.txt", "r") as f:
                    lines = f.readlines()
                    for line in lines:
                        line = line.strip()
                        vul_file_path = line.split(";")[0]
                        vulnerable_line = line.split(";")[1].split(",")[0]

                        try:
                            main([vul_file_path,
                                  vulnerable_line,
                                  vul_case_path + "pred_simu.txt",
                                  vul_case_path + "recover.txt",
                                  generated_patch_path,
                                  str(count1 * 10)])

                            vul_file_name = vul_file_path.split("/")[-1]

                            for file2 in os.listdir(generated_patch_path):
                                subprocess.call(f"diff --strip-trailing-cr {vul_file_path} {generated_patch_path}/{file2}/{vul_file_name} > {generated_patch_path}/{file2}/patch.diff",
                                                shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

                        except Exception as e:
                            print("", end="")

                        count1 += 1