#!/usr/bin/python

from changes2xml import XMLOutput, quote


class Script:
    def __init__(self, filename):
        self.id = filename.split("/")[-1].split(".")[0]

        f = open(filename, "rt")
        self.title = ""

        self.text = quote(f.read(), replaceNewLines=0)

        f.close()

        while 1:
            line, self.text = self.text.split("\n", 1)
            line = line.split("#")[-1].strip()
            if not line: break
            if self.title: self.title += " "
            self.title += line



def scripts2xml(filenames):
    output = XMLOutput("scripts")

    for filename in filenames:
        script = Script(filename)
        output.addLiteral("script", script.text, id=script.id, title=script.title)

    return output.result()


if __name__ == "__main__":

    import sys

    print scripts2xml(sys.argv[1:])
