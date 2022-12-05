import seaborn
import pandas
import matplotlib.pyplot as plt


def main():
    generate_scatterplot(r'../stats-out/SoftwareUpdate@443b7951/AvailabilityOverTime/arrivedAtServerTimeline.csv')


def generate_scatterplot(path):
    csv = pandas.read_csv(path)
    res = seaborn.scatterplot(x="timestamp", y="count", data=csv)
    plt.show()
    plt.savefig('./figures/fig_01')


if __name__ == "__main__":
    main()
