using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Entity;
using System.Data.Entity.Infrastructure;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web;
using System.Web.Http;
using System.Web.Http.Description;
using WebApi.Data;
using WebApi.Models;

namespace WebApi.Controllers
{
    /// <summary>
    /// 计步信息
    /// </summary>
    public class StepInfoesController : ApiController
    {
        private WebApiContext db = new WebApiContext();

        private static DateTime UnixStartTime = new DateTime(1970, 1, 1);
        /// <summary>
        /// 获取给定时间戳之后的所有数据，注意。实际生产环境不能这么做，当数据量巨大时，这将读取数据库中大量数据，将有可能拖垮服务器。
        /// </summary>
        /// <param name="timestamp"></param>
        /// <returns></returns>
        // GET: api/StepInfoes/5
        public List<StepInfo> Get(long timestamp)
        {

            if (HttpContext.Current.Session?["UserId"] == null)
            {
                return new List<StepInfo>();
            }
            string userId = HttpContext.Current.Session["UserId"] as string;
            var stepInfo = db.StepInfoes.Where(s => s.UserId == userId && s.TimeStamp > timestamp)
                .ToList()
                .GroupBy(s => UnixStartTime.AddMilliseconds(s.TimeStamp).ToString("M-d"))
                .Select(g => { return g.ToList().MaxItem(s => s.TodaySteps); })
                .ToList();
            return stepInfo;
        }

        /// <summary>
        /// 上传活动数据
        /// </summary>
        /// <param name="stepInfo"></param>
        /// <returns></returns>
        // POST: api/StepInfoes
        public IHttpActionResult PostStepInfo(StepInfo stepInfo)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            if (stepInfo.TodaySteps == 0)
            {
                return StatusCode(HttpStatusCode.NoContent);
            }

            if (HttpContext.Current.Session?["UserId"] == null)
            {
                return Unauthorized();
            }

            stepInfo.UserId = HttpContext.Current.Session?["UserId"].ToString();
            db.StepInfoes.Add(stepInfo);
            db.SaveChanges();

            return StatusCode(HttpStatusCode.NoContent);
        }


        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }
    }

    public class TestController : ApiController
    {
        private WebApiContext db = new WebApiContext();
        private static DateTime UnixStartTime = new DateTime(1970, 1, 1);
        public IHttpActionResult Get()
        {
            if (HttpContext.Current.Session?["UserId"] == null)
            {
                return Unauthorized();
            }
            string userId = HttpContext.Current.Session["UserId"] as string;
            var random = new Random();
            //生成近30天的数据，每天10条
            for (int i = 0; i < 30; i++)
            {
                for (int j = 0; j < 10; j++)
                {
                    var steps = random.Next(100, 10000);
                    var stepInfo = new StepInfo
                    {
                        TimeStamp = (long)(DateTime.Now - TimeSpan.FromDays(i) - UnixStartTime).TotalMilliseconds,
                        TodaySteps = steps,
                        Calories = steps * 0.1f,
                        Distance = steps * 0.3f,
                        UserId = userId
                    };
                    db.StepInfoes.Add(stepInfo);
                }
            }
            db.SaveChanges();
            return StatusCode(HttpStatusCode.NoContent);
        }
    }
    static class LinqExt
    {
        public static T MaxItem<T>(this ICollection<T> source, Func<T, IComparable> keySelect)
        {
            if (source == null || source.Count == 0)
            {
                return default(T);
            }
            T maxT = source.First();
            foreach (var t in source)
            {
                maxT = keySelect(t).CompareTo(keySelect(maxT)) >= 0 ? t : maxT;
            }

            return maxT;
        }
    }
}