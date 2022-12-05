import seaborn
import pandas
import matplotlib.pyplot as plt


def generateScatterplot(path):
    csv = pandas.read_csv(r'../../stats-out/SoftwareUpdate@443b7951/AvailabilityOverTime/arrivedAtServerTimeline.csv')
    res = seaborn.scatterplot(x="timestamp", y="count", data=csv)
    plt.show()
    plt.savefig('fig_01')
