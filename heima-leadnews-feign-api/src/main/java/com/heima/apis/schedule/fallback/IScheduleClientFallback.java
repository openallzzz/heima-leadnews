package com.heima.apis.schedule.fallback;

import com.heima.apis.schedule.IScheduleClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.schedule.dtos.Task;
import org.springframework.stereotype.Component;

@Component
public class IScheduleClientFallback implements IScheduleClient {

    /**
     * 添加延迟任务
     *
     * @param task
     * @return 返回任务在数据库中的id
     */
    @Override
    public ResponseResult addTask(Task task) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "任务添加失败");
    }

    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @Override
    public ResponseResult cancelTask(long taskId) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "任务取消失败");
    }

    /**
     * 按照类型和优先级来拉取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @Override
    public ResponseResult poll(int type, int priority) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "任务拉取失败");
    }
}
