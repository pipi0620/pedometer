using System;
using System.Collections.Generic;
using System.Data.Entity;
using System.Linq;
using System.Web;
using WebApi.Models;

namespace WebApi.Data
{
    public class WebApiContext : DbContext
    {
        // You can add custom code to this file. Changes will not be overwritten.
        // 
        // If you want Entity Framework to drop and regenerate your database
        // automatically whenever you change your model schema, please use data migrations.
        // For more information refer to the documentation:
        // http://msdn.microsoft.com/en-us/data/jj591621.aspx
    
        public WebApiContext() : base("name=WebApiContext")
        {
        }

        public System.Data.Entity.DbSet<StepInfo> StepInfoes { get; set; }

        public System.Data.Entity.DbSet<UserInfo> UserInfoes { get; set; }
    }
}
