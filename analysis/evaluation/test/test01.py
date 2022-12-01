import seaborn
import pandas
import matplotlib.pyplot as plt

csv = pandas.read_csv(r'../../stats-out/SoftwareUpdate@5387f9e0/AvailabilityOverTime/arrivedAtServerTimeline.csv')
res = seaborn.scatterplot(x="timestamp", y="count", data=csv)
plt.show()
