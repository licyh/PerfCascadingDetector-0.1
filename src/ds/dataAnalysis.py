

filename_input = '/tmp/dynamicpoints.txt'
filename_output = '/tmp/dynamicpoints.out'

set = set()

with open(filename_input, 'r') as fin:
    for line in fin.readlines():
        line = line.strip()
        str = line.split(' ', 3)[-1];
        #print(str)
        set.add(str)
        

'''
sortedlist = sorted(dict.items(), key=lambda x:x[1], reverse=True)
'''
        
with open(filename_output, 'w') as fout:
    for str in set:
        fout.write("%s\n" % str)