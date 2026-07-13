import request from '@/utils/request'

export function getAttendanceRecords(params) {
  return request.get('/attendance/record/list', { params })
}

export function getTodayAttendance(userId) {
  return request.get('/attendance/record/today', { params: { userId } })
}

export function checkIn(userId) {
  return request.post('/attendance/check-in', null, { params: { userId } })
}

export function checkOut(userId) {
  return request.post('/attendance/check-out', null, { params: { userId } })
}

export function getAttendanceStatistics(month, extra = {}) {
  return request.get('/attendance/statistics', { params: { month, ...extra } })
}

export function getShiftSchedules(params) {
  return request.get('/attendance/schedule/list', { params })
}

export function getShiftSchedulesByDate(date) {
  return request.get('/attendance/schedule/by-date', { params: { date } })
}

export function saveShiftSchedule(data) {
  return request.post('/attendance/schedule/save', data)
}

export function deleteShiftSchedule(scheduleId) {
  return request.post('/attendance/schedule/delete', null, { params: { scheduleId } })
}

export function getOperators() {
  return request.get('/attendance/operators')
}
