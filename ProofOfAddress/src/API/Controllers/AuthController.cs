using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyLocalFarmer.ProofOfAddress.Shared;

namespace MyLocalFarmer.ProofOfAddress.API.Controllers
{
    [Route("[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        [HttpGet("getcurrentuser")]
        public CurrentUser GetCurrentUser()
        {
            CurrentUser currentUser;
            if (this.User.Identity is null || !this.User.Identity.IsAuthenticated)
            {
                currentUser = new CurrentUser(false, String.Empty, new Dictionary<string, string>());
            }
            else
            {
                Dictionary<string, string> claims = new Dictionary<string, string>();
                foreach (var claim in this.User.Claims)
                {
                    claims.Add(claim.Type, claim.Value);
                }
                currentUser = new CurrentUser(this.User.Identity.IsAuthenticated, this.User.Identity.Name ?? String.Empty, claims);
            }
            return currentUser;
        }


        [HttpGet("login")]
        public IActionResult Login(string returnUrl = "/")
        {
            return new ChallengeResult("Cognito", new AuthenticationProperties()
            {
                RedirectUri = returnUrl
            });
        }

        [Authorize]
        [HttpGet("logout")]
        public IActionResult Logout(string returnUrl = "")
        {
            HttpContext.SignOutAsync();

            return new SignOutResult("Cognito", new AuthenticationProperties()
            {
                RedirectUri = returnUrl
            });
        }
    }
}
