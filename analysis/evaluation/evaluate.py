import seaborn
import pandas
import matplotlib.pyplot as plt
import os

sim_name = 'Simulation01'


def main():
    update_metrics()
    node_metrics()


def update_metrics():
    path = r'../stats-out/Simulation01/updateMetrics/'
    update_paths = os.listdir(path)
    for update_path in update_paths:
        result_paths = os.listdir(path + update_path + '/')
        for csv_name in result_paths:
            csv = pandas.read_csv(path + '/' + update_path + '/' + csv_name)
            generate_scatter_plot(csv, 'timestamp', 'count',
                                  path + '/' + update_path + '/' + csv_name.partition('.')[0] + '.png')


def node_metrics():
    path = r'../stats-out/Simulation01/nodeMetrics/'
    csv = pandas.read_csv(path + 'nodeStateMonitorTimeline.csv')
    generate_scatter_plot(csv, 'timestamp', 'nodesOnline', path + 'nodeStateMonitorTimeline.png')


def generate_scatter_plot(csv, x, y, result_fig_path):
    seaborn.scatterplot(x=x, y=y, data=csv)
    plt.savefig('./' + result_fig_path)
    plt.show()


if __name__ == "__main__":
    main()
