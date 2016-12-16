#!/usr/bin/python

import sys, getopt, csv


def createBcidTsUpdateStatements(inputFile, outputFile, resourceType):
    f = open(inputFile, 'r')
    out_f = open(outputFile, 'w')
    reader = csv.reader(f)

    headers = next(reader, None)
    idIndex = headers.index('_id')
    tsIndex = headers.index('_created')
    ercWhatIndex = headers.index('erc.what')
    dcTypeIndex = headers.index('dc.type')

    for row in reader:
        stmt = ''
        if resourceType:
            if row[ercWhatIndex] == resourceType or row[dcTypeIndex] == resourceType:
                stmt = "update bcids set ts = FROM_UNIXTIME('" + row[tsIndex] + "') where binary identifier = '" + row[idIndex] + "' and resourceType = '" + resourceType + "';\n"
        else:
            stmt = "update bcids set ts = '" + row[tsIndex] + "' where binary identifier = '" + row[idIndex] + "';\n"

        if stmt:
            out_f.write(stmt)


def main(argv):
    inputfile = ''
    outputfile = ''
    resourceType = None
    try:
        opts, args = getopt.getopt(argv, "hi:o:", ["ifile=", "ofile=", "resourceType="])
    except getopt.GetoptError:
        print('test.py -i <inputfile> -o <outputfile> -r <resourceType>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('test.py -i <inputfile> -o <outputfile> -r <resourceType>')
            sys.exit()
        elif opt in ("-i", "--ifile"):
            inputfile = arg
        elif opt in ("-o", "--ofile"):
            outputfile = arg
        elif opt in ("-r", "--resourceType"):
            resourceType = arg

    createBcidTsUpdateStatements(inputfile, outputfile, resourceType)


if __name__ == "__main__":
    main(sys.argv[1:])
