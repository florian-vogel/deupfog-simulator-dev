import shutil

import seaborn
import pandas
import matplotlib.pyplot as plt
import os

sim_name = 'Simulation01'
generate_metrics = {
    'link': True,
    'update': True,
    'node': True
}
evaluation_output_location = './../metrics'


def main():
    if os.path.isdir(evaluation_output_location):
        shutil.rmtree(evaluation_output_location)
    os.mkdir(evaluation_output_location)
    os.mkdir(evaluation_output_location + '/linkMetrics')
    os.mkdir(evaluation_output_location + '/nodeMetrics')
    os.mkdir(evaluation_output_location + '/updateMetrics')
    if generate_metrics['link']:
        link_metrics()

    if generate_metrics['update']:
        update_metrics()

    if generate_metrics['node']:
        node_metrics()


def update_metrics():
    path = r'../stats-out/Simulation01/updateMetrics/'
    update_paths = os.listdir(path)
    for update_path in update_paths:
        result_paths = os.listdir(path + update_path + '/')
        for csv_name in result_paths:
            csv = pandas.read_csv(path + '/' + update_path + '/' + csv_name)
            seaborn.relplot(data=csv, x='timestamp', y='count', kind="line")
            plt.savefig(evaluation_output_location + '/updateMetrics/' + csv_name.partition('.')[
                0] + '.png')
            plt.show()


def node_metrics():
    path = r'../stats-out/Simulation01/nodeMetrics/'
    csv = pandas.read_csv(path + 'nodeStateMonitorTimeline.csv')

    seaborn.relplot(data=csv, x='timestamp', y='nodesOnline', kind="line")
    plt.savefig(evaluation_output_location + '/nodeMetrics/nodeStateMonitorTimeline.png')
    plt.show()


def link_metrics():
    path = r'../stats-out/Simulation01/linkMetrics/'
    csv = pandas.read_csv(path + 'linkStateMonitorTimeline.csv')
    csv_m = csv.melt('timestamp', var_name='type', value_name='number')

    seaborn.relplot(data=csv_m, x='timestamp', y='number', hue='type', kind="line")
    plt.savefig(evaluation_output_location + '/linkMetrics/linkStateMonitorTimeline.png')
    plt.show()


if __name__ == "__main__":
    main()
