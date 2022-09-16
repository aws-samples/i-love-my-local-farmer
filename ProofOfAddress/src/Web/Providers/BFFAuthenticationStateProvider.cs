using Microsoft.AspNetCore.Components.Authorization;
using MyLocalFarmer.ProofOfAddress.Shared;
using MyLocalFarmer.ProofOfAddress.Web.Services;
using System.Security.Claims;

namespace MyLocalFarmer.ProofOfAddress.Web.Providers
{
    public class BFFAuthenticationStateProvider : AuthenticationStateProvider
    {
        private readonly ICurrentUserService _currentUserService;
        private readonly ILogger<BFFAuthenticationStateProvider> _logger;
        private CurrentUser? _currentUser;

        public BFFAuthenticationStateProvider(ICurrentUserService currentUserService, ILogger<BFFAuthenticationStateProvider> logger)
        {
            _currentUserService = currentUserService;
            _logger = logger;
        }

        public override async Task<AuthenticationState> GetAuthenticationStateAsync()
        {
            var identity = new ClaimsIdentity();
            try
            {
                var userInfo = await GetCurrentUser();
                if (userInfo.IsAuthenticated)
                {
                    var claims = new List<Claim>() { new Claim(ClaimTypes.Name, userInfo!.UserName) };
                    claims.AddRange(userInfo.Claims.Select(kvp => new Claim(kvp.Key, kvp.Value)));
                    identity = new ClaimsIdentity(claims, "Server authentication");
                }
            }
            catch (HttpRequestException ex)
            {
                _logger.LogInformation($"Request failed: {ex.ToString}");
            }

            return new AuthenticationState(new ClaimsPrincipal(identity));

        }

        private async Task<CurrentUser> GetCurrentUser()
        {
            if (_currentUser != null && _currentUser.IsAuthenticated) return _currentUser;
            _currentUser = await _currentUserService.GetCurrentUser();
            return _currentUser;
        }
    }
}
