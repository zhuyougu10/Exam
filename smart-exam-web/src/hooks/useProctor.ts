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
    // FIX: 初始化时直接获取当前真实的全屏状态，而不是默认 true
    const isFullscreen = ref(!!document.fullscreenElement)
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
            // 只有当前不在全屏时才请求
            if (!document.fullscreenElement) {
                await document.documentElement.requestFullscreen()
            }
            // 请求成功，更新状态
            isFullscreen.value = true
        } catch (e) {
            // FIX: 如果全屏请求被浏览器拦截（如刷新页面时），强制将状态置为 false
            // 这样会触发 ExamPaper.vue 显示遮罩，引导用户点击“恢复”按钮
            isFullscreen.value = false
            console.warn('Enter fullscreen blocked:', e)
            ElMessage.warning('为了保证考试环境，请点击页面恢复全屏模式')
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

    // 屏蔽常见按键 (F11, F12, Ctrl+P, 等)
    const preventKeys = (e: KeyboardEvent) => {
        // 屏蔽 F11 (全屏), F12 (开发者工具), Esc (退出全屏), Alt+Tab (切屏 - 浏览器无法完全屏蔽但可尝试)
        // 注意：Esc 退出全屏是浏览器强制行为，无法 preventDefault，只能通过 fullscreenchange 监听
        if (
            e.key === 'F12' ||
            e.key === 'F11' ||
            (e.ctrlKey && e.shiftKey && e.key === 'I') || // DevTools
            (e.ctrlKey && e.key === 'p') || // Print
            (e.ctrlKey && e.key === 's') || // Save
            (e.altKey) // Alt组合键
        ) {
            e.preventDefault()
            e.stopPropagation()
            // ElMessage.warning('考试期间禁止使用此按键')
        }
    }

    // 统一屏蔽交互：鼠标右键、复制、粘贴、剪切、选择
    const preventInteraction = (e: Event) => {
        e.preventDefault()
        e.stopPropagation()
        return false
    }

    onMounted(() => {
        // 1. 全屏与可见性监听
        document.addEventListener('fullscreenchange', handleFullscreenChange)
        document.addEventListener('visibilitychange', handleVisibilityChange)

        // 2. 键盘事件监听
        window.addEventListener('keydown', preventKeys)

        // 3. 鼠标与剪贴板事件监听 (新增)
        document.addEventListener('contextmenu', preventInteraction)
        document.addEventListener('copy', preventInteraction)
        document.addEventListener('paste', preventInteraction)
        document.addEventListener('cut', preventInteraction)
        document.addEventListener('selectstart', preventInteraction)

        // 每 60 秒自动保存一次答案
        saveTimer = setInterval(() => {
            saveAnswers()
        }, 60000)

        // FIX: 挂载时再次同步状态
        isFullscreen.value = !!document.fullscreenElement

        // 尝试进入全屏
        // 注意：如果是刷新页面，这里会抛出错误（被浏览器拦截），进入 catch 块
        // 从而将 isFullscreen 置为 false，正确显示遮罩
        enterFullscreen()
    })

    onUnmounted(() => {
        if (saveTimer) clearInterval(saveTimer)

        document.removeEventListener('fullscreenchange', handleFullscreenChange)
        document.removeEventListener('visibilitychange', handleVisibilityChange)

        window.removeEventListener('keydown', preventKeys)

        // 移除交互监听
        document.removeEventListener('contextmenu', preventInteraction)
        document.removeEventListener('copy', preventInteraction)
        document.removeEventListener('paste', preventInteraction)
        document.removeEventListener('cut', preventInteraction)
        document.removeEventListener('selectstart', preventInteraction)
    })

    return {
        violationCount,
        isFullscreen,
        enterFullscreen
    }
}