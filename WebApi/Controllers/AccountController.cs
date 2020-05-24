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
using System.Web.SessionState;
using WebApi.Data;
using WebApi.Models;

namespace WebApi.Controllers
{

    //*******************************************************************************************
    // 何为RESTful？简而言之：使用http的四种谓语：GET POST PUT DELETE
    //*******************************************************************************************
    // 接下来以本控制器为例进行讲解。
    // 1：本控制器下的所有API的url路径都是相同的，区别在于http谓词（或者称为http方法/操作等）不同
    // 2: GET DELETE方法可以带有url参数。POST方法一般不带URL参数。PUT方法一般既有URL参数，又含有Http Body中的参数

    // 3：GET 仅仅用于获取数据
    // 4：POST用于上传数据
    // 5：PUT用于更新数据，按照RESTful规范，PUT和POST最大的区别在于：在参数内容完全相同的情况下，执行N次PUT，和执行1次PUT操作，效果应该是相同的。执行POST操作则没有这个要求。
    // -----怎么理解？一句话概括：POST常用于添加一条数据，PUT用于更新某条已有数据。
    // 6：DELETE用于删除数据，url参数中指出需要删除的数据的ID。在本项目中未用到。
    //
    //*******************************************************************************************

    /// <summary>
    /// 账户信息。
    /// </summary>
    public class AccountController : ApiController
    {
        private WebApiContext db = new WebApiContext();


        // GET: api/Account
        [ResponseType(typeof(UserInfo))]
        public IHttpActionResult Get()
        {
            if (HttpContext.Current.Session?["UserId"] == null)
            {
                return Unauthorized();
            }

            UserInfo userInfo = db.UserInfoes.Find(HttpContext.Current.Session?["UserId"]);
            if (userInfo == null)
            {
                return NotFound();
            }

            return Ok(userInfo);
        }

        /// <summary>
        /// 更新用户信息
        /// </summary>
        /// <param name="id"></param>
        /// <param name="userInfo"></param>
        /// <returns></returns>
        // PUT: api/Account/5
        [ResponseType(typeof(void))]
        public IHttpActionResult PutUserInfo(string id, UserInfo userInfo)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }
            if (id != userInfo.Id)
            {
                return BadRequest();
            }
            if (HttpContext.Current.Session?["UserId"] == null)
            {
                return Unauthorized();
            }
            if (Equals(HttpContext.Current.Session?["UserId"], id) == false)
            {
                return Unauthorized();
            }

            var entity = db.UserInfoes.Find(id);
            if (entity == null)
            {
                return NotFound();
            }

            entity.UserName = userInfo.UserName ?? entity.UserName;
            entity.Password = userInfo.Password ?? entity.Password;
            entity.Phone = userInfo.Phone ?? entity.Phone;
            entity.DeviceToken = userInfo.DeviceToken ?? entity.DeviceToken;
            entity.Weight = userInfo.Weight > 0 ? userInfo.Weight : entity.Weight;
            entity.Height = userInfo.Height > 0 ? userInfo.Height : entity.Height;

            db.Entry(entity).State = EntityState.Modified;

            try
            {
                db.SaveChanges();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!UserInfoExists(id))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return StatusCode(HttpStatusCode.NoContent);
        }

        /// <summary>
        /// 登录或注册
        /// </summary>
        /// <param name="userInfo"></param>
        /// <returns></returns>
        // POST: api/Account
        [ResponseType(typeof(UserInfo))]
        public IHttpActionResult Post(UserInfo userInfo)
        {
            if (string.IsNullOrWhiteSpace(userInfo.Id) || string.IsNullOrWhiteSpace(userInfo.Password))
            {
                return BadRequest(ModelState);
            }

            var entity = db.UserInfoes.Find(userInfo.Id);
            if (entity == null)
            {
                return Register(userInfo);
            }

            if (entity.Password.Equals(userInfo.Password))
            {
                HttpContext.Current.Session["UserId"] = entity.Id;
                entity.DeviceToken = userInfo.DeviceToken;
                db.SaveChanges();

                userInfo.Id = entity.Id;
                userInfo.UserName = entity.UserName;
                userInfo.Weight = entity.Weight;
                userInfo.Height = entity.Height;
                userInfo.DeviceToken = entity.DeviceToken;
                userInfo.Phone = entity.Phone;
                userInfo.Password = null;

                return Ok(userInfo);
            }

            return NotFound();

        }

        private IHttpActionResult Register(UserInfo userInfo)
        {
            userInfo.UserName = userInfo.UserName ?? userInfo.Id;
            userInfo.Weight = userInfo.Weight > 0 ? userInfo.Weight : 65;
            userInfo.Height = userInfo.Height > 0 ? userInfo.Height : 170;

            db.UserInfoes.Add(userInfo);

            try
            {
                db.SaveChanges();
            }
            catch (DbUpdateException)
            {
                if (UserInfoExists(userInfo.Id))
                {
                    return Conflict();
                }
                else
                {
                    throw;
                }
            }

            userInfo.Password = null;

            return Ok(userInfo);
        }
        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }

        private bool UserInfoExists(string id)
        {
            return db.UserInfoes.Count(e => e.Id == id) > 0;
        }
    }
}