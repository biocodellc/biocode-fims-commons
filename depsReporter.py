import subprocess
import os, re

"""
This is a simple script that feeds every file in the specified directory containing compiled java classes into javap, analyzes the output,
and prints a csv of all dependencies (excluding deps that start with either "biocode/" or "java/") and the number of usages.
"""
def main():
    data = dict()
    dir = "../ppo-shadow"
    # dir = "build/classes/main/"
    pattern = re.compile(b'.* \/\/ (?!String)(?!.*biocode\/fims)(?!.* (#.*:\(\)L)?(.*\/:L)?java\/)(?!.* \w+:\()(?!.* \w+:Ljava\/)(.{2,}).*')

    for (dirpath, dirnames, filenames) in os.walk(dir):
        for filename in filenames:
            f = os.path.join(dirpath, filename)
            try:
                contents = subprocess.run(["javap", "-p", "-c", f], stdout=subprocess.PIPE).stdout

                for line in contents.splitlines():
                    m = pattern.match(line)
                    if m:
                        g = m.group(3).split(b' ')
                        key = g[0]
                        value = g[1]

                        if key in data:
                            if value in data[key]:
                                data[key][value] += 1
                            else:
                                data[key][value] = 1
                        else:
                            data[key] = {value:1}
            except:
                print(f)


    for k, v in data.items():
        for name, count in v.items():
            print("{},{},{}\n".format(k.decode('utf-8'),name.decode('utf-8'),count))



if __name__ == '__main__':
    main()