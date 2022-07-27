using MyLocalFarmer.ProofOfAddress.Shared;

namespace MyLocalFarmer.ProofOfAddress.Web.Services
{
    public interface ICurrentUserService
    {
        Task<CurrentUser> GetCurrentUser();
    }
}
