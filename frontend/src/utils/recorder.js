// 浏览器录音 → 16kHz 单声道 WAV（recorder-core），格式与后端 qwen3-asr-flash 已验证匹配。
// 需 HTTPS 或 localhost 才能访问麦克风。
import Recorder from 'recorder-core'
import 'recorder-core/src/engine/wav'

let rec = null

export function startRecording() {
  return new Promise((resolve, reject) => {
    rec = Recorder({ type: 'wav', sampleRate: 16000, bitRate: 16 })
    rec.open(
      () => { rec.start(); resolve() },
      (msg) => reject(new Error(msg || '无法打开麦克风（需 HTTPS 或 localhost 并授权）'))
    )
  })
}

export function stopRecording() {
  return new Promise((resolve, reject) => {
    if (!rec) { reject(new Error('尚未开始录音')); return }
    rec.stop(
      (blob) => { try { rec.close() } catch (e) {} rec = null; resolve(blob) },
      (msg) => { try { rec.close() } catch (e) {} rec = null; reject(new Error(msg || '录音结束失败')) }
    )
  })
}
