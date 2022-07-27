namespace MyLocalFarmer.ProofOfAddress.Shared
{
    public class CurrentUser
    {
        public bool IsAuthenticated { get; set; }
        public string UserName { get; set; }
        public IDictionary<string, string> Claims { get; set; }

        public CurrentUser()
        {
            IsAuthenticated = false;
            UserName = String.Empty;
            Claims = new Dictionary<string, string>();
        }

        public CurrentUser(bool isAuthenticated, string userName, IDictionary<string, string> claims)
        {
            IsAuthenticated = isAuthenticated;
            UserName = userName;
            Claims = claims;
        }
    }
}