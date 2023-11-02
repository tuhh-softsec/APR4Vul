# Usage 

RepairThemAll can be executed directly from the **source** or via a **Docker** image where RepairThemAll has been pre-configured and is ready to be used.

## From source

### Requirements

1. Linux or OSX
2. Java 7
3. Java 8
4. Python 2
5. Maven
6. Ant
7. wget
8. Git >= 1.9
9. SVN >= 1.8
10. Perl >= 5.0.10

### Init RepairThemAll

1. Clone this repository with `git clone --recursive https://github.com/program-repair/RepairThemAll.git`

2. Init the repository with `./init.sh`.

3. Go to `script/config.py` and update the configuration for your machine (java home, and working directory)

### Default Configuration

The default configuration is stored at `script/config.py`.

```ini
# Specify the working directory where the repair attempts will be executed
WORKING_DIRECTORY = os.path.join("/tmp/") 
# Where the results of the execution will be stored
OUTPUT_PATH = os.path.join(REPAIR_ROOT, "results/")

# Path to Z3 binary (it is used by Nopol) 
Z3_PATH = os.path.join(REPAIR_ROOT, "libs", "z3", "build")

# Path to Java 7 and Java 8 bin folders
JAVA7_HOME = expanduser("/usr/lib/jvm/java-1.7.0-openjdk-amd64/bin/")
JAVA8_HOME = expanduser("/usr/lib/jvm/java-1.8.0-openjdk-amd64/bin/")

# Java arguments that are given to the repair tools
JAVA_ARGS = "-Xmx4g -Xms1g"

# number of parallel execution thread for local execution
LOCAL_THREAD = 1
# maximum parallel in Grid5000
GRID5K_MAX_NODE = 50

# Repair Attemps timeout in minute
TOOL_TIMEOUT = "120"
```

### Execute 

Use `python script/repair.py` to run the repair tools on the benchmarks

Command line

```bash
python script/repair.py {Arja,GenProg,Kali,RSRepair,jKali,jGenProg,jMutRepair,Cardumen,Nopol,DynaMoth,NPEFix}
    --benchmark {Bears, Bugs.jar, Defects4J, IntroClassJava, QuixBugs}
    --id <bug_id> # optional, if not specified all the bugs of the benchmark will be used. The format is specific for each benchmark, and you can check the list of bugs available per benchmark with `python script/print_bugs_available.py --benchmark <benchmark_name>`
```

Example:

```bash
python script/repair.py Nopol --benchmark Defects4J --id chart-5
```

## From Docker

### Setup

1. First, install Docker ([doc](https://docs.docker.com/)).

2. Then, execute the command to download the image:

```
docker pull tdurieux/repairthemall
```

### Execute

The shortest command to run Nopol on a particular defect from Defects4J is:
```
docker run -it --rm -v <absolute_path_to_store_results>:/results tdurieux/repairthemall Nopol -b Defects4J -i Chart_5
```

### Output

The output folder can be setup in `script/config.py`. One will find there the following structure:

```
├── benchmark name
│ ├── project
│ │ ├── bug id
│ │ │ ├── tool
│ │ │ │ ├── random seed
│ │ │ │ │ ├── repair.log (stdout from the repair tool)
│ │ │ │ │ ├── result.json (see below)
│ │ │ │ │ ├── grid5k.stderr.log (on Grid5k)
│ │ │ │ │ └── detailed-result.json (available only for some repair tool)
```

The `result.json` file is structured as follows:

```javascript
{
  "repair_begin": timestamp of the beginning of the repair tool execution, 
  "repair_end": timestamp of the end of the repair tool execution, 
  "patches": [
    {
      "patch": textual representation of the diff between the buggy source code and the patched source code
      // other information depending on the repair tool
    }
  ]
}
```
