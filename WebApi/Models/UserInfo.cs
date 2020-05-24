using System;
using System.ComponentModel.DataAnnotations;
using Newtonsoft.Json;

namespace WebApi.Models
{
    public class UserInfo
    {
        public String Id { get; set; }
        public String UserName { get; set; }
        public String Password { get; set; }
        public String Phone { get; set; }
        public float Height { get; set; }
        public float Weight { get; set; }
        public string DeviceToken { get; set; }
    }
}