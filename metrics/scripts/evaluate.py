import shutil

import seaborn
import pandas
import matplotlib.pyplot as plt
import os

update_metrics_path = r'/updateMetrics'
resources_usage_metrics_path = r'/resourcesUsageMetrics'
package_loss_metrics_path = r'/packageLossMetrics'

generate_metrics = {
    'update': True,
    'resources_usage': True,
}
rel_output_path = r'./../graphs'
rel_csv_data_path = r'./../csv_data'


def main():
    if os.path.isdir(rel_output_path):
        shutil.rmtree(rel_output_path)

    simulations_csv_data = os.listdir(rel_csv_data_path)

    os.mkdir(rel_output_path)

    for simulation_csv_data_path in simulations_csv_data:
        input_path = rel_csv_data_path + '/' + simulation_csv_data_path
        output_path = rel_output_path + '/' + simulation_csv_data_path

        os.mkdir(output_path)
        os.mkdir(output_path + update_metrics_path)
        os.mkdir(output_path + resources_usage_metrics_path)

        if generate_metrics['update']:
            generate_update_metrics_graphs(input_path, output_path)

        if generate_metrics['resources_usage']:
            generate_resources_usage_graphs(input_path, output_path)


def generate_update_metrics_graphs(csv_data_path, graph_output_path):
    update_csv_data_path = csv_data_path + update_metrics_path
    update_csv_data = os.listdir(update_csv_data_path)
    for update_path in update_csv_data:
        update_csv_data_file_names_path = update_csv_data_path + '/' + update_path + '/'
        update_csv_data_file_names = os.listdir(update_csv_data_file_names_path)
        update_out_path = graph_output_path + update_metrics_path + '/' + update_path.partition('.')[2]
        os.mkdir(update_out_path)
        for csv_name in update_csv_data_file_names:
            csv_path = update_csv_data_file_names_path + '/' + csv_name
            csv = pandas.read_csv(csv_path)
            seaborn.scatterplot(data=csv, x='timestamp', y='count')
            plt.savefig(update_out_path + '/' + csv_name.partition('.')[0] + '.png')
            plt.show()


def generate_resources_usage_graphs(csv_data_path, graph_output_path):
    resources_usage_csv_data_path = csv_data_path + resources_usage_metrics_path
    resources_usage_csv_data = os.listdir(resources_usage_csv_data_path)
    for csv_name in resources_usage_csv_data:
        csv_path = resources_usage_csv_data_path + '/' + csv_name
        csv = pandas.read_csv(csv_path)
        seaborn.relplot(data=csv, x='timestamp', y='count', kind="line")
        plt.savefig(graph_output_path + resources_usage_metrics_path + '/' + csv_name.partition('.')[
            0] + '.png')
        plt.show()


if __name__ == "__main__":
    main()
