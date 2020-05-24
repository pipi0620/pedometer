using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using System.Web;
using Newtonsoft.Json;
using WebApi.Data;

namespace WebApi.Controllers
{
    /// <summary>
    /// 后台任务，在每天的固定时间向所有运动量不足的用户推送消息。
    /// </summary>
    public class BackgroundTask
    {
        public static DateTime NotifyTime = DateTime.Parse("18:00:00");
        public static int NotifyThreshold = 1000;
        private static Timer timer;
        public static void Init()
        {
            var now = DateTime.Now;
            var nowTime = now.TimeOfDay;
            var notifyTime = NotifyTime.TimeOfDay;

            var delayTime = nowTime < notifyTime ? notifyTime - nowTime : (notifyTime + TimeSpan.FromDays(1)) - nowTime;
            timer = new Timer(o => Work(), null, delayTime, TimeSpan.FromDays(1));
        }

        private static void Work()
        {
            WebApiContext db = new WebApiContext();
            var today = DateTime.Today.Ticks * 10000;

            //有运动，但是运动量不足的用户
            var sql = $@"SELECT u.* FROM UserInfoes u , 
	(SELECT  UserId,MAX(TodaySteps) steps FROM StepInfoes WHERE StepInfoes.TimeStamp>{today} GROUP BY StepInfoes.UserId) AS msteps 
WHERE 	u.Id=msteps.UserId	AND 	msteps.steps<{NotifyThreshold}";
            var devices = db.UserInfoes.SqlQuery(sql);
            foreach (var deviceToken in devices)
            {
                Notify(deviceToken.DeviceToken);
            }
            //今天没有运动记录的用户列表
            sql = $@"select u.* from UserInfoes u ,	(select u.Id from UserInfoes u ,StepInfoes s where u.Id = s.UserId and s.TimeStamp>{today} group by u.Id,s.UserId) as st
where u.Id!=st.Id";
            devices = db.UserInfoes.SqlQuery(sql);
            foreach (var deviceToken in devices)
            {
                Notify(deviceToken.DeviceToken);
            }
        }

        private static void Notify(string deviceToken)
        {
            WebRequest tRequest = WebRequest.Create("https://fcm.googleapis.com/fcm/send");
            tRequest.Method = "post";
            //serverKey - Key from Firebase cloud messaging server  
            tRequest.Headers.Add(string.Format("Authorization: key={0}", "AIzaSyDt7yULTT3gf3-hw9EIQjWvTWw3ZJDXFzI"));
            //Sender Id - From firebase project setting  
            tRequest.Headers.Add(string.Format("Sender: id={0}", "AAAAAIZ_0sw:APA91bHz_58pQ0RMivL4_Ipur4wFYeI9EdIuPDSKXHySdrPMYTQhBfwqUYIerx3WDQAWjyrsfFBhhR3mI7sVEiOcTumY6Ft5lo74RQaEg-9eFA5HX2mz5bw95lTPQSFcvbhxhe26u8eP"));
            tRequest.ContentType = "application/json";
            var payload = new
            {
                to = deviceToken,
                priority = "high",
                content_available = true,
                notification = new
                {
                    body = "You didn't exercise enough today, go for a walk!",
                    title = "Time to exercise",
                    badge = 1
                },
            };

            string postbody = JsonConvert.SerializeObject(payload).ToString();
            Byte[] byteArray = Encoding.UTF8.GetBytes(postbody);
            tRequest.ContentLength = byteArray.Length;
            using (Stream dataStream = tRequest.GetRequestStream())
            {
                dataStream.Write(byteArray, 0, byteArray.Length);
                using (WebResponse tResponse = tRequest.GetResponse())
                {
                    using (Stream dataStreamResponse = tResponse.GetResponseStream())
                    {
                        if (dataStreamResponse != null) using (StreamReader tReader = new StreamReader(dataStreamResponse))
                            {
                                String sResponseFromServer = tReader.ReadToEnd();
                                //result.Response = sResponseFromServer;
                            }
                    }
                }
            }
        }
    }
}