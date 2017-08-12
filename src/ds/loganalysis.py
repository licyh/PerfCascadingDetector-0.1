

filename_input = '/tmp/dynamicpoints.txt'
filename_output = '/tmp/dynamicpoints.out'

dict = {}

with open(filename_input, 'r') as fin:
    for line in fin.readlines():
        str = line.split(' ', 3)[-1];
        print(str)
        
'''
sortedlist = sorted(dict.items(), key=lambda x:x[1], reverse=True)

with open(filename_output, 'w') as fout:
    for tup in sortedlist:
        fout.write("(%s, %d)\n" % (tup[0], tup[1]))
'''