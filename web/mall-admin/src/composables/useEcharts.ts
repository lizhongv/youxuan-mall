import { onBeforeUnmount, onMounted, shallowRef, watch, type Ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent, TitleComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsCoreOption } from 'echarts/core'
import { useAppStore } from '@/stores/app'

// 按需引入，控制产物体积
echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, TitleComponent, GraphicComponent, CanvasRenderer])

/**
 * ECharts 生命周期封装：自适应尺寸、主题切换时重新生成配置。
 * build 函数在每次渲染时调用，内部读取 CSS 变量即可跟随深浅色。
 */
export function useEcharts(el: Ref<HTMLElement | undefined>, build: () => EChartsCoreOption) {
  const chart = shallowRef<echarts.ECharts>()
  const app = useAppStore()
  let observer: ResizeObserver | undefined

  function render() {
    if (!el.value) return
    if (!chart.value) chart.value = echarts.init(el.value)
    chart.value.setOption(build(), true)
  }

  onMounted(() => {
    render()
    if (el.value) {
      observer = new ResizeObserver(() => chart.value?.resize())
      observer.observe(el.value)
    }
  })

  // 主题切换后 CSS 变量变化，延迟一帧再读取
  watch(
    () => app.theme,
    () => requestAnimationFrame(render),
  )

  onBeforeUnmount(() => {
    observer?.disconnect()
    chart.value?.dispose()
  })

  return { chart, render }
}
