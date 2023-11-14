# APR4Vul

**Abstract**: Security vulnerability fixes could be a promising research avenue for Automated Program Repair (APR) techniques. In recent years, APR tools have been thoroughly developed for fixing generic bugs. However, the area is still relatively unexplored when it comes to fixing security bugs or vulnerabilities.

In this paper, we evaluate nine state-of-the-art APR tools and one vulnerability-specific repair tool. In particular, we investigate their ability to generate patches for 79 real-world Java vulnerabilities in the Vul4J dataset, as well as the level of trustworthiness of these patches. We evaluate the tools with respect to their ability to generate security patches that are (i) testable, (ii) having the positive effect of closing the vulnerability, and (iii) not having side effects from a functional point of view. Our results show that the evaluated APR tools were able to generate testable patches for around 20% of the considered vulnerabilities. On average, nearly 73% of the testable patches indeed eliminate the vulnerabilities, but only 44% of them could actually fix security bugs while maintaining the functionalities.

To understand the root cause of this phenomenon, we conduct a detailed comparative study of the general bug fix patterns in Defect4J and the vulnerability fix patterns in ExtraVul (which we extend from Vul4J). Our investigation shows that, although security patches are short in terms of lines of code, they contain unique characteristics in their fix patterns compared to general bugs. For example, many security fixes require adding method calls. These method calls contain specific input validation-related keywords, such as encode, normalize, and trim. In this regard, our study suggests that additional repair patterns should be implemented for existing APR tools to fix more types of security vulnerabilities.

## Selected repair tools
The table below shows the selected repair tools in our evaluation study to fix Java vulnerabilities. We used [RepairThemAll](https://github.com/program-repair/RepairThemAll) framework (already supports the tools in Arja and Astor frameworks). We extended RepairThemAll to support the new repair tools TBar, the new dataset Vul4J (as described in the next section), and the new configuration for "perfect" fault localization information to feed to the tools. SeqTrans is not integrated into RepairThemAll as it requires complicated pre- and post-processing steps for the input data and predicted patches.

*(\*)We extended the tools by adding customized Fault Localization modules and Test Executors. The last column links to the modified versions of the tools.*

### Table 1: Selected Repair Tools
| #   | Tool        | Framework                                   | Checkout SHA | Source Code(*)            |
| --- | ----------- | ------------------------------------------- | ----------- | ------------------------- |
| 1   | Arja        | [Arja](https://github.com/yyxhdy/arja)      | e795032     | [Source](apr_tools/arja)  |
| 2   | GenProg-A   | [Arja](https://github.com/yyxhdy/arja)      | e795032     | [Source](apr_tools/arja)  |
| 3   | Kali-A      | [Arja](https://github.com/yyxhdy/arja)      | e795032     | [Source](apr_tools/arja)  |
| 4   | RSRepair-A  | [Arja](https://github.com/yyxhdy/arja)      | e795032     | [Source](apr_tools/arja)  |
| 5   | jGenProg    | [Astor](https://github.com/SpoonLabs/astor) | 6278347     | [Source](apr_tools/astor) |
| 6   | jKali       | [Astor](https://github.com/SpoonLabs/astor) | 6278347     | [Source](apr_tools/astor) |
| 7   | jMutRepair  | [Astor](https://github.com/SpoonLabs/astor) | 6278347     | [Source](apr_tools/astor) |
| 8   | Cardumen    | [Astor](https://github.com/SpoonLabs/astor) | 6278347     | [Source](apr_tools/astor) |
| 9   | TBar        | [TBar](https://github.com/TruX-DTF/TBar)    | 4b5d42f     | [Source](apr_tools/TBar)  |
| 10  | SeqTrans    | [SeqTrans](https://github.com/chijianlei/SeqTrans) | 95bc295     | [Source](apr_tools/SeqTrans)  |

## Used dataset of vulnerabilities
This evaluation study uses the [Vul4J](https://github.com/bqcuong/vul4j) dataset, which was extracted from the [*"project KB"* knowledge base](https://github.com/SAP/project-kb) and contains 79 vulnerabilities from 51 real-world open-source Java projects. The vulnerabilities in Vul4J span in 25 different Common Weakness Enumeration (CWE) categories, and 35.4% of them belong to the OWASP Top 10  Web Application Security Risks.

## Repository structure
This repository is organized as follows.
```
APR4Vul
├── apr_tools: contains our extended source code of the repair tools from Arja and Astor frameworks, the TBar and SeqTrans tools
├── evaluation_data: contains data used in evaluation for all analyses in our study
│   ├── RQ1 - Repair Performance.xlsx: the performance results of the repair tools, including repair attempts, repair time, and number of e2e tested patches
│   ├── RQ2 - Manual Patch Validation.xlsx: the results of manual patch assessment done by three researchers
│   ├── RQ3 - ExtraVul Repair Actions and Repair Patterns Analyses.xlsx: the detailed counting result of repair actions in ExtraVul database by three researchers
│   └── Section 5.3 - Dataset Test Cases Analysis.xlsx: the analysis results of test cases in Vul4J
├── experiments: contains the dataset, the repair tools' artifacts, and necessary scripts (including RepairThemAll) to build the APR4Vul Docker image where we can conduct our experiments
├── repair_results: contains the repair result files from the tools
└── README.md
```

## Usage

### Install the required software
The code is deployed to run in a Docker container. Please download and install Docker on your machine first. If you use a PC or a Mac, you can find the link to download Docker Desktop at: https://www.docker.com/products/docker-desktop.

If you want to run our evaluation from **source** of RepairThemAll in this repository, please follow the below **Setup** section to build the Docker image and run it on Docker. You could also reuse our **[pre-built Docker image](https://hub.docker.com/r/vulrepair/apr4vul)**, which is **ready to be used** for running the evaluation. If you follow the second approach, you can skip the Setup section.

### Setup
In this step, after you download our replication package, extract it and see this README.md file. Please follow these steps to build APR4Vul Docker image.

1. Download Oracle Java SE Development Kit (JDK) 7. It is required to install JDK 7 manually in the Ubuntu image we use as the Docker base image. Please download the file `jdk-7u80-linux-x64.tar.gz` from [this website](https://www.oracle.com/java/technologies/javase/javase7-archive-downloads.html) and put it in the `experiments/` folder.

2. Download the pretrained model of SeqTrans from [here](https://drive.google.com/file/d/1OfN1HzLaSpOpnyBb5VkO-wXS_7g-5l0c/view) and put it in the `experiments/SeqTrans/pretrain/` folder.

3. Build the Docker image for APR4Vul.
```bash
cd experiments
docker build -t vulrepair/apr4vul .
```

### Deploy
In this step, we will deploy APR4Vul (built from source or used the [pre-built one](https://hub.docker.com/r/vulrepair/apr4vul) provided by us) to Docker. Run the below command to start the Docker container and access to it. Please remember to update `<absolute_path_to_store_results>` to the folder on your host machine that you want to store the repair results.
```bash
docker run -it \
  --name apr4vul \
  -v <absolute_path_to_store_results>:/results \
  -e JAVA_ARGS="-Xmx4g -Xms1g -XX:MaxPermSize=512m" \
  -e THREADS="1" \
  -e TOOL_TIMEOUT="120" \
  vulrepair/apr4vul
```

### Execute tools from RepairThemAll framework
After deploying APR4Vul and accessing its Docker container, you can execute the repair process of the repair tools on the VUL4J dataset as the below command.

```bash
cd /repairthemall
python script/repair.py <tool_name> --benchmark VUL4J --id <vul_id>
```

Where:
- `<tool_name>` is selected from: `Arja, GenProg, Kali, RSRepair, jGenProg, jKali, jMutRepair, Cardumen, TBar`
- `<vul_id>` is selected from [this dataset file](experiments/RepairThemAll/data/benchmarks/vul4j/vul4j.csv)

**Example:** Run TBar on the *Infinite Loop* vulnerability CVE-2018-1324 (whose internal id is VUL4J-6).
```bash
cd /repairthemall
python script/repair.py TBar --benchmark VUL4J --id VUL4J-6
```

Once the repair execution is finished, you can obtain the repair results in the configured folder (`/results` in the container machine, or the results folder you have already configured on your host machine). For the above example, you could obtain the result file at the path `/results/vul4j/apache_commons-compress/VUL4J-6/result.json` in the container machine, which contains information about:
  * `repair_begin`, `repair_end` are the timestamps of the beginning and the end of the repair execution
  * `patches` contains the list of generated patches from the repair tool, represented in textual diff (`patch` key) and other representations if available (`edits` key).

```json
{
  "repair_end": "2022-03-17 07:09:23.252376", 
  "repair_begin": "2022-03-17 07:08:28.361515", 
  "patches": [
    {
      "edits": [], 
      "patch": "//**********************************************************\n//org/apache/commons/compress/archivers/zip/X0017_StrongEncryptionHeader.java ------ 313\n//**********************************************************\n===Buggy Code===\nfor (int i = 0; i < this.rcount; i++) {\n                for (int j = 0; j < this.hashSize; j++) {\n                    //  ZipUtil.signedByteToUnsignedInt(data[offset + 16 + (i * this.hashSize) + j]));\n                }\n            }\n\n===Patch Code===\nfor (int i = 0; i==this.rcount; i++) {\n                for (int j = 0; j < this.hashSize; j++) {\n                    //  ZipUtil.signedByteToUnsignedInt(data[offset + 16 + (i * this.hashSize) + j]));\n                }\n            }"
    }
  ]
}
```
### Execute SeqTrans
To run SeqTrans, you need to have access to the APR4Vul's Docker container at first. Then, you could perform the patch prediction with SeqTrans by following these steps:

1. Perform the patch prediction
```bash
cd /seqtrans
python3 translate.py -model pretrain/model.pt -src vul4j/<vul_id>/<vul_file>_<vul_line>/abstract.txt -out vul4j/<vul_id>/<vul_file>_<vul_line>/pred_simu.txt -beam_size 10 -n_best 10
```

Where:
- `<vul_id>` is selected from [this dataset file](experiments/RepairThemAll/data/benchmarks/vul4j/vul4j.csv)
- `<vul_file>` and `<vul_line>` are selected from the folder `vul4j/<vul_id>`

2. Perform the abstraction backfill and inject the patch into the vulnerable file
```bash
cd /seqtrans
python3 patch_generation.py
```

3. After performing the above steps, ten predicted patches are located at: `vul4j/<vul_id>/<vul_file>_<vul_line>/generated_patch`

**Example:** Run SeqTrans on the *Cross-site scripting (XSS)* vulnerability CVE-2013-4378 (whose internal id is VUL4J-50).
```bash
cd /seqtrans
# patch prediction
python3 translate.py -model pretrain/model.pt -src vul4j/VUL4J-50/HtmlSessionInformationsReport_162/abstract.txt -out vul4j/VUL4J-50/HtmlSessionInformationsReport_162/pred_simu.txt -beam_size 10 -n_best 10
# abstract backfill and patch injection
python3 patch_generation.py
```

Ten predicted patches are generated at `vul4j/VUL4J-50/HtmlSessionInformationsReport_162/generated_patch`. Let's look at the first one, where is at `vul4j/VUL4J-50/HtmlSessionInformationsReport_162/generated_patch/1/patch.diff`:
```diff
162c162
< 			write(remoteAddr);
---
> 			write(htmlEncodeButNotSpace(remoteAddr));

```
