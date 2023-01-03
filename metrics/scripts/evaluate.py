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
    # todo
    'package_loss': False
}
rel_output_path = r'./../graphs'
rel_csv_data_path = r'./../csv_data'


def main():
    if os.path.isdir(rel_output_path):
        shutil.rmtree(rel_output_path)
    os.mkdir(rel_output_path)
    os.mkdir(rel_output_path + update_metrics_path)
    os.mkdir(rel_output_path + resources_usage_metrics_path)
    # os.mkdir(rel_output_path + package_loss_metrics_path)

    if generate_metrics['update']:
        generate_update_metrics_graphs()

    if generate_metrics['resources_usage']:
        generate_resources_usage_graphs()

    if generate_metrics['package_loss']:
        print('no package-loss graphs implemented')


def generate_update_metrics_graphs():
    update_csv_data_path = rel_csv_data_path + update_metrics_path
    update_csv_data = os.listdir(update_csv_data_path)
    for update_path in update_csv_data:
        update_csv_data_file_names_path = update_csv_data_path + '/' + update_path + '/'
        update_csv_data_file_names = os.listdir(update_csv_data_file_names_path)
        for csv_name in update_csv_data_file_names:
            csv_path = update_csv_data_file_names_path + '/' + csv_name
            csv = pandas.read_csv(csv_path)
            seaborn.relplot(data=csv, x='timestamp', y='count', kind="line")
            plt.savefig(rel_output_path + update_metrics_path + '/' + csv_name.partition('.')[
                0] + '.png')
            plt.show()


def generate_resources_usage_graphs():
    resources_usage_csv_data_path = rel_csv_data_path + resources_usage_metrics_path
    resources_usage_csv_data = os.listdir(resources_usage_csv_data_path)
    for csv_name in resources_usage_csv_data:
        csv_path = resources_usage_csv_data_path + '/' + csv_name
        csv = pandas.read_csv(csv_path)
        seaborn.relplot(data=csv, x='timestamp', y='count', kind="line")
        plt.savefig(rel_output_path + resources_usage_metrics_path + '/' + csv_name.partition('.')[
            0] + '.png')
        plt.show()


if __name__ == "__main__":
    main()
