namespace MyLocalFarmer.ProofOfAddress.Shared
{
    public record CurrentUser(bool IsAuthenticated, string UserName, IDictionary<string,string> Claims);
}