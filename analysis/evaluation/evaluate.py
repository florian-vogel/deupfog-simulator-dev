import seaborn
import pandas
import matplotlib.pyplot as plt
import os

sim_name = 'Simulation01'


def main():
    link_metrics()


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


def link_metrics():
    path = r'../stats-out/Simulation01/linkMetrics/'
    csv = pandas.read_csv(path + 'linkStateMonitorTimeline.csv')
    csv_m = csv.melt('timestamp', var_name='type', value_name='number')
    seaborn.relplot(data=csv_m, x='timestamp', y='number', hue='type', kind="line")

    plt.savefig('./' + path + 'linkStateMonitorTimeline.png')
    plt.show()


def generate_scatter_plot(csv, x, y, result_fig_path):
    seaborn.scatterplot(x=x, y=y, data=csv, style="line")
    plt.savefig('./' + result_fig_path)
    plt.show()


def generate_rel_plot(csv, x, ys, result_fig_path):
    for y in ys:
        seaborn.relplot(x=x, y=y, data=csv)

    plt.savefig('./' + result_fig_path)
    plt.show()


if __name__ == "__main__":
    main()
