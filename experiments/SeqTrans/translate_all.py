import os
import subprocess

PRETRAIN_MODEL = "pretrain/simulation_model.pt"
# PRETRAIN_MODEL = "pretrain/chronology_model.pt"


def main():
    start_id = "VUL4J-1"
    cont = True
    for i in range(1, 80):
        vul_id = "VUL4J-" + str(i)
        if vul_id != start_id:
            if cont:
                continue
        else:
            cont = False

        root_path = "./vul4j/" + vul_id
        for file in os.listdir(root_path):
            file_path = root_path + "/" + file
            if os.path.isdir(file_path) and file != "patch" and "_" in file:
                print(file_path)

                cmd = f"python translate.py -model {PRETRAIN_MODEL} -src {file_path}/abstract.txt -beam_size 10 -n_best 10 " \
                      f"-out {file_path}/pred_simu.txt -verbose"
                subprocess.run(cmd, shell=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

        # break


if __name__ == "__main__":
    main()
