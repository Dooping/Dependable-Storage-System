import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

secinms = 1000
data = pd.read_csv('results.csv')

bench1 = data[data.benchmark == 1]
bench2 = data[data.benchmark == 2]
bench3 = data[data.benchmark == 3]
bench4 = data[data.benchmark == 4]
bench5 = data[data.benchmark == 5]

#select all rows where the status is 200 for each benchmark
B1OKstatus = bench1[bench1.status==200]
B2OKstatus = bench2[bench2.status==200]
B3OKstatus = bench3[bench3.status==200]
B4OKstatus = bench4[bench4.status==200]
B5OKstatus = bench5[bench5.status==200]

#select all rows where the status is not 200 for each benchmark
B1FAILstatus = bench1[bench1.status!=200]
B2FAILstatus = bench2[bench2.status!=200]
B3FAILstatus = bench3[bench3.status!=200]
B4FAILstatus = bench4[bench4.status!=200]
B5FAILstatus = bench5[bench5.status!=200]

#number of ops a second for each benchmark
B1Throughput = secinms / np.mean(B1OKstatus.time.values)
B2Throughput = secinms / np.mean(B2OKstatus.time.values)
B3Throughput = secinms / np.mean(B3OKstatus.time.values)
B4Throughput = secinms / np.mean(B4OKstatus.time.values)
B5Throughput = secinms / np.mean(B5OKstatus.time.values)

#number of ops a second for each benchmark
B1Failput = secinms / np.mean(B1FAILstatus.time.values)
B2Failput = secinms / np.mean(B2FAILstatus.time.values)
B3Failput = secinms / np.mean(B3FAILstatus.time.values)
B4Failput = secinms / np.mean(B4FAILstatus.time.values)
B5Failput = secinms / np.mean(B5FAILstatus.time.values)

# data to plot
n_groups = 5
OK_meantime = (B1Throughput, B2Throughput, B3Throughput, B4Throughput, B5Throughput)
FAIL_meantime = (B1Failput, B2Failput, B3Failput, B4Failput, B5Failput)
 
# create plot
fig, ax = plt.subplots()
index = np.arange(n_groups)
bar_width = 0.35
opacity = 0.8
 
rects1 = plt.bar(index, OK_meantime, bar_width,
                 alpha=opacity,
                 color='b',
                 label='Throughput (ops/sec)')
 
rects2 = plt.bar(index + bar_width, FAIL_meantime, bar_width,
                 alpha=opacity,
                 color='r',
                 label='Failed')
 
plt.xlabel('Benchmarks')
plt.ylabel('Throughput (ops/sec)')
plt.title('Throughput by benchmark')
plt.xticks(index + bar_width, ('B1', 'B2', 'B3', 'B4','B5'))
plt.legend(loc=4)
 
plt.show()