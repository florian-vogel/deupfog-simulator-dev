import seaborn
import pandas
import matplotlib.pyplot as plt
import os

sim_name = 'Simulation01'


def main():
    update_metrics()


def update_metrics():
    path = r'../stats-out/Simulation01/updateMetrics/'
    update_paths = os.listdir(path)
    for update_path in update_paths:
        result_paths = os.listdir(path + update_path + '/')
        for csv_path in result_paths:
            csv = pandas.read_csv(path + '/' + update_path + '/' + csv_path)
            generate_scatter_plot(csv, update_path + csv_path + '.png')


def generate_scatter_plot(csv, result_fig_name):
    seaborn.scatterplot(x="timestamp", y="count", data=csv)
    plt.savefig('./' + result_fig_name)
    plt.show()


if __name__ == "__main__":
    main()
