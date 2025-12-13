import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '@/utils/request'

interface UseProctorOptions {
    recordId: () => number | undefined
    onAutoSubmit: () => void
    saveAnswers: () => void
}

export function useProctor({ recordId, onAutoSubmit, saveAnswers }: UseProctorOptions) {
    const violationCount = ref(0)
    const isFullscreen = ref(true)
    const MAX_VIOLATIONS = 3
    let saveTimer: any = null

    // 上报异常日志
    const logViolation = async (actionType: string, content?: string) => {
        const rId = recordId()
        if (!rId) return

        try {
            await request.post('/proctor/log', {
                recordId: rId,
                actionType,
                content: content || `检测到违规行为: ${actionType}`,
                imgSnapshot: '' // 预留截图字段
            })
        } catch (e) {
            console.error('监考日志上报失败', e)
        }
    }

    // 请求全屏
    const enterFullscreen = async () => {
        try {
            if (!document.fullscreenElement) {
                await document.documentElement.requestFullscreen()
            }
            isFullscreen.value = true
        } catch (e) {
            // 部分浏览器可能拦截非用户交互触发的全屏
            ElMessage.warning('请点击页面任意位置以进入全屏考试模式')
        }
    }

    // 监听全屏状态变化
    const handleFullscreenChange = () => {
        if (!document.fullscreenElement) {
            isFullscreen.value = false
            logViolation('exit_fullscreen', '考生退出了全屏模式')
        } else {
            isFullscreen.value = true
        }
    }

    // 监听切屏/页面可见性
    const handleVisibilityChange = () => {
        if (document.hidden) {
            violationCount.value++
            logViolation('switch_screen', `考生切屏或最小化窗口 (第${violationCount.value}次)`)

            if (violationCount.value > MAX_VIOLATIONS) {
                onAutoSubmit()
                ElMessageBox.alert('系统检测到您多次切屏或离开考试页面，已触发自动交卷。', '严重违规', {
                    confirmButtonText: '确定',
                    type: 'error',
                    showClose: false,
                    closeOnClickModal: false,
                    closeOnPressEscape: false
                })
            } else {
                ElMessage.warning({
                    message: `警告：检测到切屏！这是第 ${violationCount.value} 次警告，累计 ${MAX_VIOLATIONS + 1} 次将自动交卷。`,
                    duration: 5000,
                    type: 'warning'
                })
            }
        }
    }

    // 屏蔽常见按键
    const preventKeys = (e: KeyboardEvent) => {
        // 屏蔽 F11 (全屏), F12 (开发者工具), Esc (退出全屏), Alt+Tab (切屏 - 浏览器无法完全屏蔽但可尝试)
        // 注意：Esc 退出全屏是浏览器强制行为，无法 preventDefault，只能通过 fullscreenchange 监听
        if (
            e.key === 'F12' ||
            e.key === 'F11' ||
            (e.ctrlKey && e.shiftKey && e.key === 'I') || // DevTools
            (e.altKey) // Alt 组合键
        ) {
            e.preventDefault()
            // ElMessage.warning('考试期间禁止使用此按键')
        }
    }

    // 禁用右键菜单
    const preventContextMenu = (e: Event) => {
        e.preventDefault()
    }

    onMounted(() => {
        document.addEventListener('fullscreenchange', handleFullscreenChange)
        document.addEventListener('visibilitychange', handleVisibilityChange)
        window.addEventListener('keydown', preventKeys)
        window.addEventListener('contextmenu', preventContextMenu)

        // 每 60 秒自动保存一次答案
        saveTimer = setInterval(() => {
            saveAnswers()
        }, 60000)

        // 尝试进入全屏
        enterFullscreen()
    })

    onUnmounted(() => {
        if (saveTimer) clearInterval(saveTimer)
        document.removeEventListener('fullscreenchange', handleFullscreenChange)
        document.removeEventListener('visibilitychange', handleVisibilityChange)
        window.removeEventListener('keydown', preventKeys)
        window.removeEventListener('contextmenu', preventContextMenu)
    })

    return {
        violationCount,
        isFullscreen,
        enterFullscreen
    }
}