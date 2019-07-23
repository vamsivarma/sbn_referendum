import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from matplotlib import dates

from datetime import datetime

#Distribution over time Tweets
data_y = pd.read_csv("yes_tt.csv")
data_y = data_y.iloc[:,:].values
data_n = pd.read_csv("no_tt.csv")
data_n = data_n.iloc[:,:].values
data = np.append(data_y,data_n)


ts = int("1284101485")



#convert strings into datetime objects
conv_time_y = [datetime.utcfromtimestamp(int(int(i)/1000)).strftime('%Y-%m-%d %H:%M:%S') for i in data_y]

#print(conv_time)



df = pd.DataFrame(conv_time_y, columns=['yes_tweets'])

df["yes_tweets"] = pd.to_datetime(df["yes_tweets"])

#print(df)

print(df.groupby(by=[df["yes_tweets"].dt.month, df["yes_tweets"].dt.day]).count().plot(kind="bar"))
plt.show()


conv_time_n = [datetime.utcfromtimestamp(int(int(i)/1000)).strftime('%Y-%m-%d %H:%M:%S') for i in data_n]

#print(conv_time)



df = pd.DataFrame(conv_time_n, columns=['no_tweets'])

df["no_tweets"] = pd.to_datetime(df["no_tweets"])

#print(df)

print(df.groupby(by=[df["no_tweets"].dt.month, df["no_tweets"].dt.day]).count().plot(kind="bar"))
plt.show()




conv_time_all = [datetime.utcfromtimestamp(int(int(i)/1000)).strftime('%Y-%m-%d %H:%M:%S') for i in data]

#print(conv_time)



df = pd.DataFrame(conv_time_all, columns=['all_tweets'])

df["all_tweets"] = pd.to_datetime(df["all_tweets"])

#print(df)

print(df.groupby(by=[df["all_tweets"].dt.month, df["all_tweets"].dt.day]).count().plot(kind="bar"))
plt.show()

#df.groupby(by=[b.index.month, b.index.year])
