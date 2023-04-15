import React from 'react';
import produce from 'immer';
import { ChartProps, ECharts } from '../../builder';
import { PlusData } from '../../builder/utils';


const defaultOption = {
  tooltip: {},
  legend: {
    type: 'scroll',
    orient: 'horizontal',
    bottom: 5,
    data: [],
  },
  series: [
    {
      type: 'pie',
      radius: ['50%', '70%'],
      avoidLabelOverlap: false,
      label: {
        show: false,
        position: 'center',
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '20',
          fontWeight: 'bold',
        },
      },
      labelLine: {
        show: false,
      },
      data: [],
    },
  ],
};

export function Donut({ data, ...rest }: ChartProps) {
  const [options, setOptions] = React.useState(defaultOption);

  React.useEffect(() => {
    const { types: dimensions, data: source, formatter } = PlusData(data);
    setOptions(
      produce((draft: any) => {
        draft.legend.data = dimensions;
        draft.series[0].data = source.map(({ x, y, ...rest }) => ({ ...rest, name: x, value: y }));
        draft.tooltip.valueFormatter = formatter;
      }),
    );
  }, [data]);

  return <ECharts options={options} {...(rest as any)} />;
}

